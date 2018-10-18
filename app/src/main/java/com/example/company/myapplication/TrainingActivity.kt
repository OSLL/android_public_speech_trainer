package com.example.company.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.preference.PreferenceManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.os.*
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_training.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

const val AUDIO_RECORDING = "audio_recording"
const val RECORD_AUDIO_PERMISSION = 200 // change constant?
const val RECORDING_FOLDER = "public_speech_trainer/recordings" // temporary name?
const val SPEECH_RECOGNITION_SERVICE_DEBUGGING = "test_speech_rec" // информация о взаимодействии с сервисом распознавания речи
const val SPEECH_RECOGNITION_INFO = "test_speech_info" // информация о распознавание речи (скорость чтения, номер страницы, распознанный текст)

class TrainingActivity : AppCompatActivity() {

    private var renderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var audioFile: File
    private lateinit var directory: File
    private var finishedRecording = false

    private var isCancelled = false

    private var mPlayer: MediaPlayer? = null

    @SuppressLint("UseSparseArrays")
    var TimePerSlide = HashMap<Int, Long>()

    //speech recognizer part
    private var presentationEntries = HashMap<Int,Float?>()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isAudio = sharedPreferences.getBoolean("deb_speech_audio", false)

        time = intent.getLongExtra(TIME_ALLOTTED_FOR_TRAINING, 0)

        // Запись звука мешает работе speech recognizer, а именно mediaRecorder.start().
        // SpeechRecognizer начинает бесконечно запускаться, не останавливая старые экземпляры;
        // после mediaRecorder.stop() его работа нормализуется;
        // error service - nullPointerException;
        //initAudioRecording()

        addPermission()

        //finish.isEnabled = false

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mIntent = Intent(this@TrainingActivity,SpeechRecognitionService::class.java)

        startRecognizingService()

        if(!isAudio) {
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

                    val slideReadSpeed: Float = if (curText == "") 0f else
                            curText.split(" ").size.toFloat() / TimePerSlide[curPageNum]!!.toFloat() * 60f

                    presentationEntries[curPageNum++] = slideReadSpeed

                    Log.d(SPEECH_RECOGNITION_INFO, "page number: " + (curPageNum-1).toString())
                    Log.d(SPEECH_RECOGNITION_INFO, "recognized text: $curText")
                    Log.d(SPEECH_RECOGNITION_INFO, "reading speed: " + presentationEntries[curPageNum-1].toString())

                    ALL_RECOGNIZED_TEXT += " $curText"
                    curText = ""
                    speechRecognitionService!!.setMESSAGE("")
                    audioManager!!.isMicrophoneMute = false
                    finish.isEnabled = true
                }, 2000)

            }
        }

        finish.setOnClickListener{
            /*if (!finishedRecording) {
                stopAudioRecording()
                finishedRecording = true
            }*/
            if(isAudio) {
                mPlayer?.stop()
            }
            timer(1,1).onFinish()
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

        val arr = arrayOf(Manifest.permission.RECORD_AUDIO)

        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arr,
                    1)
        }
    }

    private fun startRecognizingService(){
        Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING,"startRecognizingService called")
        audioManager!!.isMicrophoneMute = false
        try {
            taskServiceAnswer = TaskServiceAnswer()
            taskServiceAnswer!!.execute()
        } catch (e: NullPointerException) {
            Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING,  "start service error: " + e.toString() + ", service status: " + taskServiceAnswer!!.status.toString())
        }
    }

    private fun stopRecognizingService(waitForRecognitionComplete: Boolean){
        if (!waitForRecognitionComplete) {
            Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING,"stopRecognizingService called, without waiting for recognition to finish")
            try {
                taskServiceAnswer!!.setEXECUTE_FLAG(false)
                taskServiceAnswer!!.cancel(false)
            } catch (e: NullPointerException) {
                Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING,"stop service error: " + e.toString() + ", service status: " + taskServiceAnswer!!.status.toString())
            }
        }
        else {
            Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING,"stopRecognizingService called, with waiting for recognition to finish")
            try {
                val min = lastSlideTime.substring(0,lastSlideTime.indexOf("m") - 1)
                val sec = lastSlideTime.substring(lastSlideTime.indexOf(":") + 2, lastSlideTime.indexOf("s") - 1)

                time -= min.toLong() * 60 + sec.toLong()
                TimePerSlide[curPageNum] = time

                presentationEntries[curPageNum] = if (curText == "") 0f
                    else curText.split(" ").size.toFloat() / TimePerSlide[curPageNum]!!.toFloat() * 60f

            } catch (e: Exception) {
                Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING, "(stop service) put presentation entry error: " + e.toString())
            }

            ALL_RECOGNIZED_TEXT += curText

            Log.d(SPEECH_RECOGNITION_INFO, "page number: " + (curPageNum).toString())
            Log.d(SPEECH_RECOGNITION_INFO, "recognized text: $curText")
            Log.d(SPEECH_RECOGNITION_INFO, "reading speed: " + presentationEntries[curPageNum].toString())

            audioManager!!.isMicrophoneMute = false
            try {
                taskServiceAnswer!!.setEXECUTE_FLAG(false)
                taskServiceAnswer!!.cancel(false)
            } catch (e: Exception) {
                Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING,"stop service error: " + e.toString() + ", service status: " + taskServiceAnswer!!.status.toString())
            }
        }
    }

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING,"Service Connection: bind service")
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

        override fun onPreExecute() {
            Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING,"onPreExecute TaskServiceAnswer")
            try {
                bindService(mIntent, mConnection, Service.BIND_AUTO_CREATE)
                EXECUTE_FLAG = true
            } catch (e: Exception) {
                Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING, "onPreExecute Async Task error: " + e.toString())
            }
        }

        override fun onPostExecute(aVoid: Void?) {
            Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING,"onPostExecute TaskServiceAnswer")
            if (mBound) {
                unbindService(mConnection)
                mBound = false
                Log.d("testService", "onPostExecute")
            }
        }

        override fun onProgressUpdate(vararg values: Void) {
            try {
                curText = speechRecognitionService!!.getMESSAGE()
            } catch (e: Exception) {
                Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING, "onProgressUpdate Async Task error: " + e.toString())
            }
        }

        override fun doInBackground(vararg voids: Void): Void? {
            while (EXECUTE_FLAG) {
                try {
                    TimeUnit.MILLISECONDS.sleep(150)
                } catch (e: InterruptedException) {
                    Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING, "doInBackGround error: " + e.printStackTrace())
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

        val TrainingTime = intent.getLongExtra(TIME_ALLOTTED_FOR_TRAINING, 0)
        timer(TrainingTime * 1000, 1000).start()
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
                        stopRecognizingService(true)

                        val builder = AlertDialog.Builder(this@TrainingActivity)
                        builder.setMessage(R.string.training_completed)
                        builder.setPositiveButton(R.string.training_statistics) { _, _ ->
                            val stat = Intent(this@TrainingActivity, TrainingStatisticsActivity::class.java)

                            stat.putExtra(getString(R.string.presentationEntries), presentationEntries)
                            unmuteSound()
                            startActivity(stat)
                        }
                        val dialog: AlertDialog = builder.create()
                        dialog.show()
                    }, 2500)
                } catch (e: Exception) {
                    Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING, "onFinish handler error: " + e.toString())
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
        val uri = intent.getParcelableExtra<Uri>(URI)

        try {
            val temp = File(this.cacheDir, "tempImage.pdf")
            val fos = FileOutputStream(temp)
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val isPres = sharedPreferences.getBoolean("deb_pres", false)
            val ins: InputStream
            ins = if(!isPres) {
                val cr = contentResolver
                cr.openInputStream(uri)
            } else {
                assets.open(debugSlides)
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

    private fun initAudioRecording() {
        val sharedPref = getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
        val defaultValue = false
        val isRecordingOn = sharedPref.getBoolean(getString(R.string.audio_recording), defaultValue)

        if (isRecordingOn) {
            addPermissionsForAudioRecording()
        } else {
            finishedRecording = true
        }
    }

    private fun initMediaRecorder() {
        mediaRecorder = MediaRecorder()
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB)
        val parent = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            Environment.getExternalStorageDirectory()
        } else {
            filesDir
        }

        directory = File("${parent.path}${File.separator}$RECORDING_FOLDER")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        try {
            audioFile = File(directory, "recording-${getCurrentDateForName()}.amr")
            audioFile.createNewFile()
        } catch (e: IOException) {
            Log.e("error", "unable to create audio file for recording")
        }
        mediaRecorder.setOutputFile(audioFile.absolutePath)

        try {
            mediaRecorder.prepare()
        } catch (e: IOException) {
            Log.e("error", "unable to record audio")
        }
    }

    private fun startAudioRecording() {
        initMediaRecorder()
        mediaRecorder.start()
        Log.i(AUDIO_RECORDING, "started audio recording at ${getCurrentDateForLog()}")
    }

    private fun stopAudioRecording() {
        mediaRecorder.stop()
        mediaRecorder.release()
        Log.i(AUDIO_RECORDING, "finished audio recording at ${getCurrentDateForLog()}")
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(audioFile.path)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            .toInt()
        Log.i(
            AUDIO_RECORDING, "audio file length: " +
                    "${formatNumberTwoDigits(duration / 1000 / 60 / 60)}:" +
                    "${formatNumberTwoDigits(duration / 1000 / 60 % 60)}:" +
                    formatNumberTwoDigits(duration / 1000 % 60)
        )
        Log.i(AUDIO_RECORDING, "audio file path: ${directory.absolutePath}")
        Log.i(AUDIO_RECORDING, "audio file name: ${audioFile.name}")
    }

    // used for naming the audio recording file
    private fun getCurrentDateForName(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }

    // used for logging
    private fun getCurrentDateForLog(): String {
        return SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    private fun formatNumberTwoDigits(number: Int): String {
        return String.format("%02d", number)
    }

    private fun addPermissionsForAudioRecording() {
        val recordingPermissionStatus =
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        val storingPermissionStatus =
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        val permissionsToRequest = mutableListOf<String>()
        if (recordingPermissionStatus != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (storingPermissionStatus != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                    this, permissionsToRequest.toTypedArray(),
                    RECORD_AUDIO_PERMISSION
            )
        } else {
            startAudioRecording()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startAudioRecording()
            }
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

