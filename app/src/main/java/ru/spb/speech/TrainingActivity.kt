package ru.spb.speech

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.arch.lifecycle.MutableLiveData
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_training.*
import kotlinx.coroutines.*
import ru.spb.speech.appSupport.*
import ru.spb.speech.database.interfaces.PresentationDataDao
import ru.spb.speech.database.helpers.TrainingDBHelper
import ru.spb.speech.database.helpers.TrainingSlideDBHelper
import ru.spb.speech.database.*
import ru.spb.speech.firebase.FirebaseHelper
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer

const val SPEECH_RECOGNITION_SERVICE_DEBUGGING = "test_speech_rec.TrainingActivity" // информация о взаимодействии с сервисом распознавания речи
const val ACTIVITY_TRAINING_NAME = ".TrainingActivity"

@Suppress("DEPRECATION")
class TrainingActivity : AppCompatActivity() {
    private var timeOfSlide: Long = 0

    private var pdfReader: PdfToBitmap? = null

    private var isCancelled = false

    private var timeIsOver = false

    private var extraTime: Long = 0

    private var isTrainingFinish = false

    private var mPlayer: MediaPlayer? = null

    private var finishFlag = false

    @SuppressLint("UseSparseArrays")

    //speech recognizer part
    private var curText = ""
    private var mIntent: Intent? = null
    private var speechRecognitionService: SpeechRecognitionService? = null
    internal var mBound = false
    private var taskServiceAnswer: TaskServiceAnswer? = null
    private var audioManager: AudioManager? = null
    private var lastSlideTime: String = ""
    private var allRecognizedText = "" //Текст для PIE CHART

    private var time: Long = 0.toLong()

    private var presentationDataDao: PresentationDataDao? = null
    private var presentationData: PresentationData? = null

    private var trainingData: TrainingData? = null
    private var trainingSlideDBHelper: TrainingSlideDBHelper? = null

    private var mainTimer: CountDownTimer? = null
    private var extraTimeTimer: Timer? = null
    private var timerTimeRemain: Long = 0

    private var nIndex: Int = -1

    var isAudio: Boolean? = null

    private lateinit var progressHelper: ProgressHelper

    private var firebaseHelper: FirebaseHelper? = null

    private lateinit var sharedPreferences: SharedPreferences

    private val defaultSP by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    private var audioAnalyzer: AudioAnalyzer? = null
    private val audioAnalyzerController = MutableLiveData<AudioAnalyzer.AudioAnalyzerState>()
            .apply { value = AudioAnalyzer.AudioAnalyzerState.START_RECORD }

    @SuppressLint("LongLogTag", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

        if (defaultSP.getBoolean(getString(R.string.audio_analyze), true)) {
            GlobalScope.launch {
                audioAnalyzer = AudioAnalyzer.getInstance(this@TrainingActivity, audioAnalyzerController)
            }
        }

        progressHelper = ProgressHelper(this, training_activity_root_view, listOf())

        presentationDataDao = SpeechDataBase.getInstance(this)?.PresentationDataDao()
        val presId = intent.getIntExtra(getString(R.string.CURRENT_PRESENTATION_ID),-1)
        if (presId > 0) {
            presentationData = presentationDataDao?.getPresentationWithId(presId)
        }
        else {
            Log.d(APST_TAG + ACTIVITY_TRAINING_NAME, "training_act: wrong ID")
            return
        }

        trainingData = TrainingData()
        trainingSlideDBHelper = TrainingSlideDBHelper(this)

        time = presentationData?.timeLimit!!

        pdfReader = PdfToBitmap(presentationData!!, this)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        isAudio = sharedPreferences.getBoolean(getString(R.string.deb_speech_audio_key), false)

        addPermission()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mIntent = Intent(this@TrainingActivity,SpeechRecognitionService::class.java)

        startRecognizingService()

        initFireBaseHelper()

        if(!isAudio!!) {
            muteSound() // mute для того, чтобы не было слышно звуков speech recognizer
        } else {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 50, 0)
            mPlayer = MediaPlayer.create(this, debugSpeechAudio)
            mPlayer?.start()
            mPlayer?.setOnCompletionListener { stopPlay() }
        }

        if (presentationData?.pageCount == 1) {
            next.isEnabled = false
            next.alpha = 0.3f

            pause_button_training_activity.isEnabled = false
            pause_button_training_activity.alpha = 0.3f
        }

        curSlide.text = "1/${presentationData?.pageCount}"
        next.setOnClickListener {
            audioAnalyzerController.value = AudioAnalyzer.AudioAnalyzerState.NEXT_SLIDE
            next.isEnabled = false
            next.alpha = 0.3f

            pause_button_training_activity.isEnabled = false
            pause_button_training_activity.alpha = 0.3f

            finish.isEnabled = false
            finish.alpha = 0.3f

            audioManager!!.isMicrophoneMute = true
            Toast.makeText(this, "Saving State, Please Wait", Toast.LENGTH_SHORT).show()

            val index = pdfReader?.getPageIndexStatus()
            if (index != null) {
                val handler = Handler()
                handler.postDelayed({
                    nIndex = index
                    slide.setImageBitmap(pdfReader?.getBitmapForSlide(nIndex + 1))

                    val tsd = TrainingSlideData()

                    curSlide.text = "${nIndex + 2}/${presentationData?.pageCount}"

                    val min = time_left.text.toString().substring(0, time_left.text.indexOf("m") - 1)
                    val sec = time_left.text.toString().substring(
                            time_left.text.indexOf(":") + 2,
                            time_left.text.indexOf("s") - 1
                    )

                    tsd.spentTimeInSec = timeOfSlide

                    tsd.knownWords = curText

                    if (audioAnalyzer != null)
                        tsd.updateAudioStatistics(audioAnalyzer!!.getLastSlideInfo())

                    trainingSlideDBHelper?.addTrainingSlideInDB(tsd,trainingData!!)

                    allRecognizedText += " $curText"
                    curText = ""
                    speechRecognitionService!!.setMESSAGE("")
                    audioManager!!.isMicrophoneMute = false

                    finish.isEnabled = true
                    finish.alpha = 1f

                    Log.d("test_pr", "page count: ${pdfReader?.getPageCount()!!}, currentPage: ${pdfReader?.getPageIndexStatus()!!}")
                    if (pdfReader?.getPageCount()!! > (pdfReader?.getPageIndexStatus()!! + 1)) {
                        next.isEnabled = true
                        next.alpha = 1f

                        pause_button_training_activity.isEnabled = true
                        pause_button_training_activity.alpha = 1f
                    }
                    timeOfSlide = 0
                }, 2000)

            }
        }

        finish.setOnClickListener{
            audioAnalyzerController.value = AudioAnalyzer.AudioAnalyzerState.FINISH
            finishFlag = true
            mainTimer?.cancel()
            extraTimeTimer?.cancel()
            time_left.setText(R.string.training_completed)

            finish.isEnabled = false
            finish.alpha = 0.3f

            next.isEnabled = false
            next.alpha = 0.3f

            pause_button_training_activity.isEnabled = false
            pause_button_training_activity.alpha = 0.3f

            audioManager!!.isMicrophoneMute = true
            Toast.makeText(this@TrainingActivity, "Completion...", Toast.LENGTH_SHORT).show()

            try {
                val handler = Handler()
                handler.postDelayed({
                    if(isAudio!!) {
                        mPlayer?.stop()
                    }
                    stopRecognizingService(true, saveTrainingInDB = true)
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)

                    val builder = AlertDialog.Builder(this@TrainingActivity)
                    builder.setMessage(R.string.training_completed)
                    builder.setPositiveButton(R.string.training_statistics) { _, _ ->
                        val stat = Intent(this@TrainingActivity, TrainingStatisticsActivity::class.java)
                        stat.putExtra(getString(R.string.CURRENT_PRESENTATION_ID), presentationData?.id)
                        stat.putExtra(getString(R.string.CURRENT_TRAINING_ID),SpeechDataBase.getInstance(
                                this@TrainingActivity)?.TrainingDataDao()?.getLastTraining()?.id)
                        stat.putExtra(getString(R.string.count_of_slides),nIndex)

                        unMuteSound()

                        startActivity(stat)
                        finish()
                    }

                    val dialog: AlertDialog = builder.create()
                    dialog.show()
                }, 2500)
            } catch (e: Exception) {
                Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING + ACTIVITY_TRAINING_NAME, "onFinish handler error: " + e.toString())
            }
            isTrainingFinish = true
        }

        pause_button_training_activity.setOnClickListener {
            if (pause_button_text_training_activity.text.toString() == getString(R.string.continue_)) {
                next.visibility = View.VISIBLE
                finish.visibility = View.VISIBLE
                pause_button_text_training_activity.text = getString(R.string.pause)

                mainTimer?.start()
                if(timeIsOver)
                    extraTimerFun()
                startRecognizingService()

                if (isAudio!!) { mPlayer?.start(); unMuteSound()}
                else muteSound()
                return@setOnClickListener
            }

            if (isAudio!!) mPlayer?.pause()

            next.visibility = View.GONE
            finish.visibility = View.GONE
            pause_button_text_training_activity.text = getString(R.string.continue_)

            pause_button_training_activity.isEnabled = false
            pause_button_training_activity.alpha = 0.3f

            mainTimer?.cancel()
            extraTimeTimer?.cancel()
            muteSound()
            mainTimer = timer(timerTimeRemain, 1000)

            audioManager!!.isMicrophoneMute = true
            Toast.makeText(this, "Pause", Toast.LENGTH_LONG).show()

            Handler().postDelayed({
                stopRecognizingService(true, saveTrainingInDB = false)
                audioManager!!.isMicrophoneMute = false

                pause_button_training_activity.isEnabled = true
                pause_button_training_activity.alpha = 1f
            }, 2000)
        }
        mainTimer = timer(time * 1000, 1000)
        mainTimer?.start()
    }

    private  fun muteSound(){
        val mManager= getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true)
        mManager.setStreamMute(AudioManager.STREAM_ALARM, true)
        mManager.setStreamMute(AudioManager.STREAM_MUSIC, true)
        mManager.setStreamMute(AudioManager.STREAM_RING, true)
        mManager.setStreamMute(AudioManager.STREAM_SYSTEM, true)
    }

    private fun unMuteSound(){
        val mManager= getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false)
        mManager.setStreamMute(AudioManager.STREAM_ALARM, false)
        mManager.setStreamMute(AudioManager.STREAM_MUSIC, false)
        mManager.setStreamMute(AudioManager.STREAM_RING, false)
        mManager.setStreamMute(AudioManager.STREAM_SYSTEM, false)
    }

    private fun addPermission() {
        val permissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        val loadPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        val arr = arrayOf(Manifest.permission.RECORD_AUDIO)

        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arr,
                    1)
        }

        if (loadPerm != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arr,
                    1)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            super.onBackPressed()
            return true
        }
        return false
    }

    @SuppressLint("LongLogTag")
    private fun startRecognizingService(){
        Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING + ACTIVITY_TRAINING_NAME,"startRecognizingService called")
        audioManager!!.isMicrophoneMute = false
        try {
            taskServiceAnswer = TaskServiceAnswer()
            taskServiceAnswer!!.execute()
        } catch (e: NullPointerException) {
            Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING + ACTIVITY_TRAINING_NAME,  "start service error: " + e.toString() + ", service status: " + taskServiceAnswer!!.status.toString())
        }
    }

    @SuppressLint("LongLogTag")
    fun stopRecognizingService(waitForRecognitionComplete: Boolean, saveTrainingInDB: Boolean){
        if (!waitForRecognitionComplete) {
            Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING + ACTIVITY_TRAINING_NAME,"stopRecognizingService called, without waiting for recognition to finish")
            try {
                taskServiceAnswer!!.setExecuteFlag(false)
                taskServiceAnswer!!.cancel(false)
            } catch (e: NullPointerException) {
                Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING + ACTIVITY_TRAINING_NAME,"stop service error: " + e.toString() + ", service status: " + taskServiceAnswer!!.status.toString())
            }
        }
        else {
            Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING,"stopRecognizingService called, with waiting for recognition to finish")
            try {
                val tsd = TrainingSlideData()
                tsd.spentTimeInSec = timeOfSlide
                tsd.knownWords = curText
                if (audioAnalyzer != null)
                    tsd.updateAudioStatistics(audioAnalyzer!!.getLastSlideInfo())
                trainingSlideDBHelper?.addTrainingSlideInDB(tsd,trainingData!!)

                val list = trainingSlideDBHelper?.getAllSlidesForTraining(trainingData!!)
                if (list == null) {
                    Log.d(APST_TAG + ACTIVITY_TRAINING_NAME, "train act: slides == null")
                } else {
                    for (i in 0..(list.size - 1)) {
                        Log.d(APST_TAG + ACTIVITY_TRAINING_NAME, "train act, L $i : ${list[i]}")
                    }
                }


            } catch (e: Exception) {
                Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING, "(stop service) put presentation entry error: " + e.toString())
            }

            allRecognizedText += curText

            val trainingStatisticsData = TrainingStatisticsData(this, presentationData, trainingData)

            trainingData?.allRecognizedText = allRecognizedText
            trainingData?.timeStampInSec = System.currentTimeMillis() / 1000
            trainingData?.exerciseTimeFactorMarkX = (trainingStatisticsData.xExerciseTimeFactor * this.resources.getInteger(R.integer.transfer_to_interest)/this.resources.getDimension(R.dimen.number_of_factors)).format(1).replace(",", ".")
            trainingData?.speechSpeedFactorMarkY = (trainingStatisticsData.ySpeechSpeedFactor * this.resources.getInteger(R.integer.transfer_to_interest)/this.resources.getDimension(R.dimen.number_of_factors)).format(1).replace(",", ".")
            trainingData?.timeOnSlidesFactorMarkZ = (trainingStatisticsData.zTimeOnSlidesFactor * this.resources.getInteger(R.integer.transfer_to_interest)/this.resources.getDimension(R.dimen.number_of_factors)).format(1).replace(",", ".")
            trainingData?.trainingGrade = trainingStatisticsData.trainingGrade.format(resources.getInteger(R.integer.num_of_dec_in_the_training_score)).replace(",", ".")

            trainingData?.allRecognizedText = allRecognizedText
            trainingData?.timeStampInSec = System.currentTimeMillis() / 1000

            if (saveTrainingInDB) {
                val trainingDBHelper = TrainingDBHelper(this)
                trainingDBHelper.addTrainingInDB(trainingData!!, presentationData!!)
                uploadLastTrainingToFireBase()
            }

            audioManager!!.isMicrophoneMute = false
            try {
                taskServiceAnswer!!.setExecuteFlag(false)
                taskServiceAnswer!!.cancel(false)
            } catch (e: Exception) {
                Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING + ACTIVITY_TRAINING_NAME,"stop service error: " + e.toString() + ", service status: " + taskServiceAnswer!!.status.toString())
            }
        }
    }

    private val mConnection = object : ServiceConnection {
        @SuppressLint("LongLogTag")
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING + ACTIVITY_TRAINING_NAME,"Service Connection: bind service")
            val binder = service as SpeechRecognitionService.LocalBinder
            speechRecognitionService = binder.service
            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mBound = false
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class TaskServiceAnswer : AsyncTask<Void, Void, Void>() {
        private var executeFlag = true

        @SuppressLint("LongLogTag")
        override fun onPreExecute() {
            Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING + ACTIVITY_TRAINING_NAME,"onPreExecute TaskServiceAnswer")
            try {
                bindService(mIntent, mConnection, Service.BIND_AUTO_CREATE)
                executeFlag = true
            } catch (e: Exception) {
                Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING + ACTIVITY_TRAINING_NAME, "onPreExecute Async Task error: " + e.toString())
            }
        }

        @SuppressLint("LongLogTag")
        override fun onPostExecute(aVoid: Void?) {
            Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING + ACTIVITY_TRAINING_NAME,"onPostExecute TaskServiceAnswer")
            if (mBound) {
                unbindService(mConnection)
                mBound = false
                Log.d("testService", "onPostExecute")
            }
        }

        @SuppressLint("LongLogTag")
        override fun onProgressUpdate(vararg values: Void) {
            try {
                curText = speechRecognitionService!!.getMESSAGE()
            } catch (e: Exception) {
                Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING + ACTIVITY_TRAINING_NAME, "onProgressUpdate Async Task error: " + e.toString())
            }
        }

        @SuppressLint("LongLogTag", "WrongThread")
        override fun doInBackground(vararg voids: Void): Void? {
            while (executeFlag) {
                try {
                    TimeUnit.MILLISECONDS.sleep(150)
                } catch (e: InterruptedException) {
                    Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING + ACTIVITY_TRAINING_NAME, "doInBackGround error: " + e.printStackTrace())
                }

                publishProgress()
            }
            this.onPostExecute(null)
            return null
        }

        fun setExecuteFlag(EXECUTE_FLAG: Boolean) {
            this.executeFlag = EXECUTE_FLAG
        }
    }

    private fun stopPlay() {
        mPlayer?.stop()
        try {
            mPlayer?.prepare()
            mPlayer?.seekTo(0)
        } catch (t: Throwable) {
            Toast.makeText(this, t.message, Toast.LENGTH_SHORT).show()
        }

    }

    override fun onStart() {
        super.onStart()
        if(nIndex == -1)
            slide.setImageBitmap(pdfReader?.getBitmapForSlide(0))
        else
            slide.setImageBitmap(pdfReader?.getBitmapForSlide(nIndex+1))

        if(nIndex == -1)
            slide.setImageBitmap(pdfReader?.getBitmapForSlide(0))
        else
            slide.setImageBitmap(pdfReader?.getBitmapForSlide(nIndex+1))

    }

    private fun timer(millisInFuture: Long, countDownInterval: Long): CountDownTimer {
        return object : CountDownTimer(millisInFuture, countDownInterval) {

            override fun onTick(millisUntilFinished: Long) {
                timeOfSlide++
                timerTimeRemain = millisUntilFinished
                val timeRemaining = timeString(millisUntilFinished)
                if (isCancelled) {
                    if (lastSlideTime.isEmpty())
                        lastSlideTime = time_left.text.toString()
                    cancel()
                } else {
                    time_left.text = timeRemaining
                }

                if(time_left.text == getString(R.string.first_sec)){
                    val handler = Handler()
                    timeIsOver = true

                    handler.postDelayed({
                        timeOfSlide++
                        if(!finishFlag) {
                            time_left.text = getString(R.string.zero_sec)
                        }
                    },1000)

                    handler.postDelayed({
                        timeOfSlide += 2
                        time_left.setTextColor(resources.getColor(android.R.color.holo_red_light))
                        extraTimerFun()
                    },2000)

                }
            }

            @SuppressLint("LongLogTag")
            override fun onFinish() {
                isCancelled = true
            }
        }
    }

    private fun extraTimerFun(){
        extraTimeTimer = fixedRateTimer(name = "extraTime-timer",
                initialDelay = 0, period = 1000) {
            runOnUiThread {
                if(!finishFlag) {
                    time_left.text = timeString(extraTime * 1000)
                }
            }
            extraTime += 1
            timeOfSlide++
        }
    }

    @SuppressLint("UseSparseArrays")
    private fun timeString(millisUntilFinished: Long): String {

        var millisUntilFinishedVar: Long = millisUntilFinished

        val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinishedVar)
        millisUntilFinishedVar -= TimeUnit.MINUTES.toMillis(minutes)

        val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinishedVar)

        // Format the string
        return String.format(
                Locale.getDefault(),
                "%02d min: %02d sec",
                minutes, seconds
        )
    }

    private fun uploadLastTrainingToFireBase() {
        if (firebaseHelper == null) return

        GlobalScope.launch {
            async {
                firebaseHelper!!.uploadLastTraining(presentationData!!)
            }
        }
    }

    private fun initFireBaseHelper() {
        if (sharedPreferences.getBoolean(getString(R.string.useStatistics), false)) {
            GlobalScope.launch {
                firebaseHelper = withContext(Dispatchers.IO) { FirebaseHelper(this@TrainingActivity) }

                if (sharedPreferences.getBoolean(getString(R.string.firstSyncFlag), true)) {
                    sharedPreferences.edit().putBoolean(getString(R.string.firstSyncFlag), false).apply()

                    async(Dispatchers.IO) {
                        firebaseHelper!!.registerNewTester()
                        firebaseHelper!!.synchronizeAllData()
                    }
                }
            }
        }
    }

    override fun onPause() {
        progressHelper.show()
        if (pause_button_text_training_activity.text.toString() != getString(R.string.continue_) && !isTrainingFinish) {
            pause_button_training_activity.performClick()
        }
        audioAnalyzerController.value = AudioAnalyzer.AudioAnalyzerState.PAUSE
        super.onPause()
    }

    override fun onResume() {
        progressHelper.hide()
        audioAnalyzerController.value = AudioAnalyzer.AudioAnalyzerState.RESUME
        super.onResume()
    }

    override fun onDestroy() {
        stopRecognizingService(false, saveTrainingInDB = true)
        audioAnalyzerController.value = AudioAnalyzer.AudioAnalyzerState.FINISH
        unMuteSound()
        pdfReader?.finish()
        super.onDestroy()
    }

}