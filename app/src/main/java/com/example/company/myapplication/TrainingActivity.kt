package com.example.company.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.media.AudioManager
import android.net.Uri
import android.opengl.Visibility
import android.os.Bundle
import android.os.CountDownTimer
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_training.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

class TrainingActivity : AppCompatActivity() {

    private var renderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    private var isCancelled = false

    @SuppressLint("UseSparseArrays")
    var TimePerSlide = HashMap <Int, Long>()

    private var PresentEntries = HashMap<Int,Float?>()
    private var curPageNum = 1
    private var curText = ""

    private var mSpeechRecognizer:SpeechRecognizer? = null
    private var mSpeechRecognizerIntent: Intent? = null
    private var mBufferSpeechRecognizer: SpeechRecognizer? = null
    private var mBufferSpeechRecognizerIntent: Intent? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

        var time = intent.getLongExtra(TIME_ALLOTTED_FOR_TRAINING, 0)

        AddPermission()
        //muteSound() // mute sound, for unmute use unmuteSound()

        //init main recognizer
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        mSpeechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        mSpeechRecognizerIntent!!.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        mSpeechRecognizerIntent!!.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault())

        mSpeechRecognizer!!.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(bundle: Bundle) {

            }

            override fun onBeginningOfSpeech() {

            }

            override fun onRmsChanged(v: Float) {

            }

            override fun onBufferReceived(bytes: ByteArray) {

            }

            override fun onEndOfSpeech() {
                mBufferSpeechRecognizer!!.startListening(mBufferSpeechRecognizerIntent)
            }

            override fun onError(i: Int) {
                //mBufferSpeechRecognizer!!.startListening(mBufferSpeechRecognizerIntent)
                Log.d("speechT", "MAIN RECOGNIZER ERROR")
            }

            override fun onResults(bundle: Bundle) {
                val matches = bundle
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                if (matches != null) {
                    curText += matches[0]
                }
            }

            override fun onPartialResults(bundle: Bundle) {

            }

            override fun onEvent(i: Int, bundle: Bundle) {

            }
        })

        //init buffer recognizer
        mBufferSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        mBufferSpeechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        mBufferSpeechRecognizerIntent!!.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        mBufferSpeechRecognizerIntent!!.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault())

        mBufferSpeechRecognizer!!.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(bundle: Bundle) {

            }

            override fun onBeginningOfSpeech() {

            }

            override fun onRmsChanged(v: Float) {

            }

            override fun onBufferReceived(bytes: ByteArray) {

            }

            override fun onEndOfSpeech() {
                mSpeechRecognizer!!.startListening(mSpeechRecognizerIntent)
            }

            override fun onError(i: Int) {
                Log.d("speechT", "BUFFER RECOGNIZER ERROR")
            }

            override fun onResults(bundle: Bundle) {
                val matches = bundle
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                if (matches != null) {
                    curText += matches[0]
                }
            }

            override fun onPartialResults(bundle: Bundle) {

            }

            override fun onEvent(i: Int, bundle: Bundle) {

            }
        })


        mSpeechRecognizer!!.startListening(mSpeechRecognizerIntent)

        finish.isEnabled = false

        next.setOnClickListener {
            val index = currentPage?.index
            if(renderer != null && index != null) {
                val NIndex: Int = index
                renderPage(NIndex + 1)

                val min = time_left.text.toString().substring(0, time_left.text.indexOf("m")-1)
                val sec = time_left.text.toString().substring(time_left.text.indexOf(":") + 2,
                        time_left.text.indexOf("s")-1)

                time -= min.toLong()*60 + sec.toLong()
                TimePerSlide [index+1] = time

                time = min.toLong()*60 + sec.toLong()

                mSpeechRecognizer!!.stopListening()
                mBufferSpeechRecognizer!!.stopListening()
                mSpeechRecognizer!!.startListening(mSpeechRecognizerIntent)


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

            val SlideReadSpeed: Float
            if (curText == "")
                SlideReadSpeed = 0f
            else
                SlideReadSpeed = curText.split(" ").size.toFloat() / time!!.toFloat() * 60f

            PresentEntries.put(curPageNum++,SlideReadSpeed)

            timer(1,1).onFinish()
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
        val TrainingTime = intent.getLongExtra(TIME_ALLOTTED_FOR_TRAINING, 0)
        timer(TrainingTime*1000,1000).start()
    }

    private fun timer(millisInFuture:Long,countDownInterval:Long): CountDownTimer {
        return object: CountDownTimer(millisInFuture,countDownInterval){

            override fun onTick(millisUntilFinished: Long){
                val timeRemaining = timeString(millisUntilFinished)
                if (isCancelled){
                    time_left.setText(R.string.training_completed)
                    cancel()
                }else{
                    time_left.text = timeRemaining
                }
            }
            override fun onFinish() {
                isCancelled = true
                timer(1,1).cancel()
                val builder = AlertDialog.Builder(this@TrainingActivity)
                builder.setMessage(R.string.training_completed)
                builder.setPositiveButton(R.string.training_statistics){ _, _->
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
    private fun timeString(millisUntilFinished:Long):String{

        var millisUntilFinishedVar:Long = millisUntilFinished

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
                minutes,seconds
        )
    }

    private fun renderPage(pageIndex: Int){

        currentPage?.close()

        currentPage = renderer?.openPage(pageIndex)
        val width = currentPage?.width
        val height = currentPage?.height
        val index = currentPage?.index
        val pageCount = renderer?.pageCount
        if(width != null && height != null && index != null && pageCount != null) {
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

    private fun initRenderer(){
        val uri = intent.getParcelableExtra<Uri>(URI)

        try{
            val temp = File(this.cacheDir, "tempImage.pdf")
            val fos = FileOutputStream(temp)
            val cr = contentResolver
            val ins = cr.openInputStream(uri)

            val buffer = ByteArray(1024)

            var readBytes = ins.read(buffer)
            while(readBytes != -1){
                fos.write(buffer, 0, readBytes)
                readBytes = ins.read(buffer)
            }

            fos.close()
            ins.close()

            parcelFileDescriptor = ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(parcelFileDescriptor)
        } catch (e: IOException){
            Toast.makeText(this, "error in opening presentation file", Toast.LENGTH_LONG).show()
            Log.d("error","error in opening presentation file")
        }
    }

    private  fun muteSound(){
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
    }

    private fun unmuteSound(){
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC), AudioManager.FLAG_SHOW_UI)
    }

    override fun onPause() {
        if(isFinishing){
            currentPage?.close()
            try{
                parcelFileDescriptor?.close()
            } catch (e: IOException){
                Toast.makeText(this, "error in closing FileDescriptor", Toast.LENGTH_LONG).show()
                Log.d("error","error in closing FileDescriptor")
            }
            renderer?.close()
        }
        super.onPause()
    }
}