package com.example.company.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.ParcelFileDescriptor
import android.support.v7.app.AlertDialog
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_training.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class TrainingActivity : AppCompatActivity() {

    private var renderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    private var isCancelled = false

    @SuppressLint("UseSparseArrays")
    var TimePerSlide = HashMap <Int, Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

        var time = intent.getLongExtra(TIME_ALLOTTED_FOR_TRAINING, 0)

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
            }
        }

        finish.setOnClickListener{
            timer(1,1).onFinish()
        }
    }

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
                builder.setPositiveButton(R.string.training_statistics){_,_->
                    val stat = Intent(this@TrainingActivity, TrainingStatisticsActivity::class.java)
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
