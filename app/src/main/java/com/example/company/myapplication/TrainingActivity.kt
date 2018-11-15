package com.example.company.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.example.company.myapplication.DBTables.helpers.TrainingDBHelper
import com.example.company.myapplication.DBTables.helpers.TrainingSlideDBHelper
import com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces.PresentationDataDao
import com.example.putkovdimi.trainspeech.DBTables.PresentationData
import com.example.putkovdimi.trainspeech.DBTables.SpeechDataBase
import com.example.putkovdimi.trainspeech.DBTables.TrainingData
import com.example.putkovdimi.trainspeech.DBTables.TrainingSlideData
import kotlinx.android.synthetic.main.activity_training.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

const val SPEECH_RECOGNITION_SERVICE_DEBUGGING = "test_speech_rec.TrainingActivity" // информация о взаимодействии с сервисом распознавания речи
const val ACTIVITY_TRAINING_NAME = ".TrainingActivity"

class TrainingActivity : AppCompatActivity() {

    private var renderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    private var isCancelled = false

    private var mPlayer: MediaPlayer? = null

    @SuppressLint("UseSparseArrays")
    var TimePerSlide = HashMap<Int, Long>()

    //speech recognizer part
    private var curPageNum = 1
    private var curText = ""
    private var mIntent: Intent? = null
    private var speechRecognitionService: SpeechRecognitionService? = null
    internal var mBound = false
    private var taskServiceAnswer: TaskServiceAnswer? = null
    private var audioManager: AudioManager? = null
    private var lastSlideTime: String = ""
    private var ALL_RECOGNIZED_TEXT = "" //Текст для PIE CHART

    private var time: Long = 0.toLong()

    private var presentationDataDao: PresentationDataDao? = null
    private var presentationData: PresentationData? = null

    private var trainingData: TrainingData? = null
    private var trainingSlideDBHelper: TrainingSlideDBHelper? = null

    var isAudio: Boolean? = null

    @SuppressLint("LongLogTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

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

        saveImage()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        isAudio = sharedPreferences.getBoolean(getString(R.string.deb_speech_audio_key), false)


        addPermission()

        //finish.isEnabled = false

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mIntent = Intent(this@TrainingActivity,SpeechRecognitionService::class.java)

        startRecognizingService()

        if(!isAudio!!) {
            muteSound() // mute для того, чтобы не было слышно звуков speech recognizer
        } else {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 50, 0)
            mPlayer = MediaPlayer.create(this, debugSpeechAudio)
            mPlayer?.start()
            mPlayer?.setOnCompletionListener { stopPlay() }
        }

        next.setOnClickListener {
            next.isEnabled = false
            finish.isEnabled = false
            audioManager!!.isMicrophoneMute = true
            Toast.makeText(this, "Saving State, Please Wait", Toast.LENGTH_SHORT).show()

            val index = currentPage?.index
            if (renderer != null && index != null) {
                val handler = Handler()
                handler.postDelayed({
                    val NIndex: Int = index
                    renderPage(NIndex + 1)

                    val min = time_left.text.toString().substring(0, time_left.text.indexOf("m") - 1)
                    val sec = time_left.text.toString().substring(
                        time_left.text.indexOf(":") + 2,
                        time_left.text.indexOf("s") - 1
                    )

                    time -= min.toLong() * 60 + sec.toLong()
                    TimePerSlide[index + 1] = time
                    time = min.toLong()*60 + sec.toLong()

                    val tsd = TrainingSlideData()
                    tsd.spentTimeInSec = TimePerSlide[curPageNum]!!
                    tsd.knownWords = curText
                    trainingSlideDBHelper?.addTrainingSlideInDB(tsd,trainingData!!)

                    ALL_RECOGNIZED_TEXT += " $curText"
                    curText = ""
                    speechRecognitionService!!.setMESSAGE("")
                    audioManager!!.isMicrophoneMute = false
                    finish.isEnabled = true
                }, 2000)

            }
        }

        finish.setOnClickListener{
            timer(1,1).onFinish()
        }
    }

    @SuppressLint("SetWorldReadable")
    private fun saveImage() {

        val temp = File(this.cacheDir, "tempImage.pdf")

        parcelFileDescriptor = ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_WRITE)
        renderer = PdfRenderer(parcelFileDescriptor)

        currentPage = renderer?.openPage(0)
        val width = currentPage?.width
        val height = currentPage?.height
        if (width != null && height != null) {
            val defW = 397f
            val defH = 298f

            val coeff = height.toFloat()/width.toFloat()

            val curW = defW.toInt()
            val curH = (defW * coeff).toInt()

            bmpBase = Bitmap.createBitmap(curW, curH, Bitmap.Config.ARGB_8888)

            val whitePaint = Paint()
            whitePaint.style = Paint.Style.FILL
            whitePaint.color = Color.WHITE

            val BmpToWhite = Canvas(bmpBase)
            BmpToWhite.drawPaint(whitePaint)

            currentPage?.render(bmpBase, null,null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
        }

    }

    //speech recognizer =====

    private  fun muteSound(){
        var amanager= getSystemService(Context.AUDIO_SERVICE) as AudioManager
        amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true)
        amanager.setStreamMute(AudioManager.STREAM_ALARM, true)
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, true)
        amanager.setStreamMute(AudioManager.STREAM_RING, true)
        amanager.setStreamMute(AudioManager.STREAM_SYSTEM, true)
    }

    private fun unmuteSound(){
        var amanager= getSystemService(Context.AUDIO_SERVICE) as AudioManager
        amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false)
        amanager.setStreamMute(AudioManager.STREAM_ALARM, false)
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false)
        amanager.setStreamMute(AudioManager.STREAM_RING, false)
        amanager.setStreamMute(AudioManager.STREAM_SYSTEM, false)
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
    fun stopRecognizingService(waitForRecognitionComplete: Boolean){
        if (!waitForRecognitionComplete) {
            Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING + ACTIVITY_TRAINING_NAME,"stopRecognizingService called, without waiting for recognition to finish")
            try {
                taskServiceAnswer!!.setEXECUTE_FLAG(false)
                taskServiceAnswer!!.cancel(false)
            } catch (e: NullPointerException) {
                Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING + ACTIVITY_TRAINING_NAME,"stop service error: " + e.toString() + ", service status: " + taskServiceAnswer!!.status.toString())
            }
        }
        else {
            Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING,"stopRecognizingService called, with waiting for recognition to finish")
            try {
                val min = lastSlideTime.substring(0,lastSlideTime.indexOf("m") - 1)
                val sec = lastSlideTime.substring(lastSlideTime.indexOf(":") + 2, lastSlideTime.indexOf("s") - 1)

                time -= min.toLong() * 60 + sec.toLong()
                TimePerSlide[curPageNum] = time

                val tsd = TrainingSlideData()
                tsd.spentTimeInSec = TimePerSlide[curPageNum]!!
                tsd.knownWords = curText
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

            ALL_RECOGNIZED_TEXT += curText

            trainingData?.allRecognizedText = ALL_RECOGNIZED_TEXT
            trainingData?.timeStampInSec = System.currentTimeMillis() / 1000

            val trainingDBHelper = TrainingDBHelper(this)
            trainingDBHelper.addTrainingInDB(trainingData!!,presentationData!!)

            val list = trainingDBHelper.getAllTrainingsForPresentation(presentationData!!)
            if (list != null) {
                for (i in 0..(list!!.size - 1)) {
                    Log.d(APST_TAG + ACTIVITY_TRAINING_NAME, "train act, T $i : ${list[i]}")
                }
            } else {
                Log.d(APST_TAG + ACTIVITY_TRAINING_NAME, "train act: list == null")
            }

            audioManager!!.isMicrophoneMute = false
            try {
                taskServiceAnswer!!.setEXECUTE_FLAG(false)
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
        private var EXECUTE_FLAG = true

        @SuppressLint("LongLogTag")
        override fun onPreExecute() {
            Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING + ACTIVITY_TRAINING_NAME,"onPreExecute TaskServiceAnswer")
            try {
                bindService(mIntent, mConnection, Service.BIND_AUTO_CREATE)
                EXECUTE_FLAG = true
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

        @SuppressLint("LongLogTag")
        override fun doInBackground(vararg voids: Void): Void? {
            while (EXECUTE_FLAG) {
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

        fun setEXECUTE_FLAG(EXECUTE_FLAG: Boolean) {
            this.EXECUTE_FLAG = EXECUTE_FLAG
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

        initRenderer()
        renderPage(0)

        //initAudioRecording()

        timer(time * 1000, 1000).start()
    }

    private fun timer(millisInFuture: Long, countDownInterval: Long): CountDownTimer {
        return object : CountDownTimer(millisInFuture, countDownInterval) {

            override fun onTick(millisUntilFinished: Long) {
                val timeRemaining = timeString(millisUntilFinished)
                if (isCancelled) {
                    if (lastSlideTime.isEmpty())
                        lastSlideTime = time_left.text.toString()
                    time_left.setText(R.string.training_completed)
                    cancel()
                } else {
                    time_left.text = timeRemaining
                }
            }

            @SuppressLint("LongLogTag")
            override fun onFinish() {
                timer(1, 1).cancel()

                isCancelled = true
                finish.isEnabled = false
                next.isEnabled = false
                audioManager!!.isMicrophoneMute = true
                Toast.makeText(this@TrainingActivity, "Completion...", Toast.LENGTH_SHORT).show()

                try {
                    val handler = Handler()
                    handler.postDelayed({
                        if(isAudio!!) {
                            mPlayer?.stop()
                        }
                        stopRecognizingService(true)

                        val builder = AlertDialog.Builder(this@TrainingActivity)
                        builder.setMessage(R.string.training_completed)
                        builder.setPositiveButton(R.string.training_statistics) { _, _ ->
                            val stat = Intent(this@TrainingActivity, TrainingStatisticsActivity::class.java)
                            stat.putExtra(getString(R.string.CURRENT_PRESENTATION_ID), presentationData?.id)
                            stat.putExtra(getString(R.string.CURRENT_TRAINING_ID),SpeechDataBase?.getInstance(
                                    this@TrainingActivity)?.TrainingDataDao()?.getLastTraining()?.id)

                            unmuteSound()

                            startActivity(stat)
                        }

                        val dialog: AlertDialog = builder.create()
                        dialog.show()
                    }, 2500)
                } catch (e: Exception) {
                    Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING + ACTIVITY_TRAINING_NAME, "onFinish handler error: " + e.toString())
                }
            }
        }
    }

    @SuppressLint("UseSparseArrays")
    private fun timeString(millisUntilFinished: Long): String {

        var millisUntilFinishedVar: Long = millisUntilFinished

/*
        val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
        millisUntilFinished -= TimeUnit.HOURS.toMillis(hours)
*/

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

    private fun renderPage(pageIndex: Int) {
        currentPage?.close()

        currentPage = renderer?.openPage(pageIndex)
        val width = currentPage?.width
        val height = currentPage?.height
        val index = currentPage?.index
        val pageCount = renderer?.pageCount
        if (width != null && height != null && index != null && pageCount != null) {
            val NWidth: Int = width
            val NHeight: Int = height
            val NIndex: Int = index
            val NPageCount: Int = pageCount
            val bitmap: Bitmap = Bitmap.createBitmap(NWidth, NHeight, Bitmap.Config.ARGB_8888)
            currentPage?.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            slide.setImageBitmap(bitmap)
            next.isEnabled = NIndex + 1 < NPageCount
        }
    }

    private fun initRenderer() {
        val uri = Uri.parse(presentationData?.stringUri)

        try {
            val temp = File(this.cacheDir, "tempImage.pdf")
            val fos = FileOutputStream(temp)
            val ins: InputStream
            ins = if(presentationData?.debugFlag == 0) {
                val cr = contentResolver
                cr.openInputStream(uri)
            } else {
                assets.open(getString(R.string.deb_pres_name))
            }

            val buffer = ByteArray(1024)

            var readBytes = ins.read(buffer)
            while (readBytes != -1) {
                fos.write(buffer, 0, readBytes)
                readBytes = ins.read(buffer)
            }

            fos.close()
            ins.close()

            parcelFileDescriptor =
                    ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(parcelFileDescriptor)
        } catch (e: IOException) {
            Toast.makeText(this, "error in opening presentation file", Toast.LENGTH_LONG).show()
            Log.d("error", "error in opening presentation file")
        }
    }

    override fun onPause() {
        if (isFinishing) {
            currentPage?.close()
            try {
                parcelFileDescriptor?.close()
            } catch (e: IOException) {
                Toast.makeText(this, "error in closing FileDescriptor", Toast.LENGTH_LONG).show()
                Log.d("error", "error in closing FileDescriptor")
            }
            renderer?.close()
        }
        super.onPause()
    }

    override fun onDestroy() {
        stopRecognizingService(false)
        super.onDestroy()
    }
}