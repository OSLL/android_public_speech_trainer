package com.example.company.myapplication

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_training.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class TrainingActivity : AppCompatActivity() {

    private var renderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

        next.setOnClickListener {
            val index = currentPage?.index
            if(renderer != null && index != null) {
                val NIndex: Int = index
                renderPage(NIndex + 1)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        initRenderer()
        renderPage(0)
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
        }
    }

    private fun initRenderer(){
        val uri = intent.getParcelableExtra<Uri>("presentation_uri")

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
        } catch(e: IOException){
            Toast.makeText(this, "error in opening presentation file", Toast.LENGTH_SHORT).show()
            Log.d("error","error in opening presentation file")
        }
    }

    override fun onPause() {
        if(isFinishing){
            currentPage?.close()
            try{
                parcelFileDescriptor?.close()
            } catch (e: IOException){
                Toast.makeText(this, "error in closing FileDescriptor", Toast.LENGTH_SHORT).show()
                Log.d("error","error in closing FileDescriptor")
            }
            renderer?.close()
        }
        super.onPause()
    }
}
