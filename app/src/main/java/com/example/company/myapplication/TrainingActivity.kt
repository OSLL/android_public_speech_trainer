package com.example.company.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

const val AUDIO_RECORDING = "audio_recording"
const val RECORD_AUDIO_PERMISSION = 200 // change constant?
const val RECORDING_FOLDER = "public_speech_trainer/recordings" // temporary name?

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

    private var PresentEntries = HashMap<Int,Float?>()
    private var curPageNum = 1
    private var curText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        val sPref = getPreferences(Context.MODE_PRIVATE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)



        var time = intent.getLongExtra(TIME_ALLOTTED_FOR_TRAINING, 0)

        AddPermission()

        if(sPref.getInt(getString(R.string.DEBUG_AUDIO), debugSpeechAudio) == -1) {
            muteSound() // mute для того, чтобы не было слышно звуков speech recognizer
        } else {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 50, 0)
            val AudTrack = R.raw.philstone
            val mPlayer = MediaPlayer.create(this, sPref.getInt(getString(R.string.DEBUG_AUDIO), debugSpeechAudio))
            mPlayer.start()
            mPlayer.setOnCompletionListener { stopPlay() }
        }

        initAudioRecording()

        val mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val mSpeechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault())

        mSpeechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(bundle: Bundle) {

            }

            override fun onBeginningOfSpeech() {

            }

            override fun onRmsChanged(v: Float) {

            }

            override fun onBufferReceived(bytes: ByteArray) {

            }

            override fun onEndOfSpeech() {

            }

            override fun onError(i: Int) {
            }

            override fun onResults(bundle: Bundle) {
                val matches = bundle
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                if (matches != null) {
                    curText = matches[0]
                }
            }

            override fun onPartialResults(bundle: Bundle) {

            }

            override fun onEvent(i: Int, bundle: Bundle) {

            }
        })

        mSpeechRecognizer.startListening(mSpeechRecognizerIntent)

        finish.isEnabled = false

        next.setOnClickListener {
            val index = currentPage?.index
            if (renderer != null && index != null) {
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

                mSpeechRecognizer.stopListening()
                mSpeechRecognizer.startListening(mSpeechRecognizerIntent)

                val SlideReadSpeed: Float
                if (curText == "")
                    SlideReadSpeed = 0f
                else
                    SlideReadSpeed = curText.split(" ").size.toFloat() / TimePerSlide[curPageNum]!!.toFloat() * 60f

                PresentEntries.put(curPageNum++,SlideReadSpeed)

                Log.d("speechT", "NUMBER OF PAGE: " + (curPageNum-1).toString())
                Log.d("speechT", "TEXT: " + curText)
                Log.d("speechT", "read speed: " + PresentEntries.get(curPageNum-1).toString())
                curText = ""
            }
        }

        finish.setOnClickListener{
            if(sPref.getInt(getString(R.string.DEBUG_AUDIO), debugSpeechAudio) != -1) {
                mPlayer?.stop()
            }
            if (!finishedRecording) {
                stopAudioRecording()
                finishedRecording = true
            }
            val SlideReadSpeed: Float
            if (curText == "")
                SlideReadSpeed = 0f
            else
                SlideReadSpeed = curText.split(" ").size.toFloat() / time.toFloat() * 60f

            PresentEntries.put(curPageNum++,SlideReadSpeed)

            timer(1,1).onFinish()
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

    //speech recognizer =====
    private fun AddPermission() {
        val permissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)

        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO),
                    1)
        }
    }
    

//======================

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
                    time_left.setText(R.string.training_completed)
                    cancel()
                } else {
                    time_left.text = timeRemaining
                }
            }

            override fun onFinish() {
                isCancelled = true
                timer(1, 1).cancel()
                val builder = AlertDialog.Builder(this@TrainingActivity)
                builder.setMessage(R.string.training_completed)
                builder.setPositiveButton(R.string.training_statistics) { _, _ ->
                    val stat = Intent(this@TrainingActivity, TrainingStatisticsActivity::class.java)

                    stat.putExtra(getString(R.string.presentationEntries), PresentEntries)

                    unmuteSound()
                    startActivity(stat)
                }
                val dialog: AlertDialog = builder.create()
                dialog.show()
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
            finish.isEnabled = !next.isEnabled
        }
    }

    private fun initRenderer() {
        val uri = intent.getParcelableExtra<Uri>(URI)

        try {
            val temp = File(this.cacheDir, "tempImage.pdf")
            val fos = FileOutputStream(temp)
            val cr = contentResolver
            val ins = cr.openInputStream(uri)

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

}
