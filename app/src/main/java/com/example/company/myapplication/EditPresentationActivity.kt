package com.example.company.myapplication

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces.PresentationDataDao
import com.example.putkovdimi.trainspeech.DBTables.PresentationData
import com.example.putkovdimi.trainspeech.DBTables.SpeechDataBase
import kotlinx.android.synthetic.main.activity_edit_presentation.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream



const val DEFAULT_TIME = "DefTime"

class EditPresentationActivity : AppCompatActivity() {

    private var renderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    private var presentationDataDao: PresentationDataDao? = null
    private var presentationData: PresentationData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_presentation)

        presentationDataDao = SpeechDataBase.getInstance(this)?.PresentationDataDao()
        val presId = intent.getIntExtra(getString(R.string.CURRENT_PRESENTATION_ID),-1)
        if (presId > 0) {
            presentationData = presentationDataDao?.getPresentationWithId(presId)
            Log.d("test_row", "edit act: $presentationData")
        }
        else {
            Log.d(TEST_DB, "edit_pres_act: wrong ID")
            return
        }

        addPresentation.setOnClickListener{

            val uri = Uri.parse(presentationData?.stringUri)

            if (presentationName.text.toString() == ""){
                Toast.makeText(this, R.string.message_no_presentation_name, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }



            val i = Intent(this, PresentationActivity::class.java)
            presentationData?.pageCount = renderer?.pageCount
            presentationData?.name = presentationName.text.toString()
            presentationDataDao?.updatePresentation(presentationData!!)

            i.putExtra(getString(R.string.CURRENT_PRESENTATION_ID), presentationData?.id)
            startActivity(i)
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            initRenderer()
            renderPage(0)
        }catch (e: Exception) {
            Log.d("test_row", "edit pres : $e" )
        }
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
            val bitmap: Bitmap = Bitmap.createBitmap(NWidth, NHeight, Bitmap.Config.ARGB_8888)
            currentPage?.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pdf_view.setImageBitmap(bitmap)
        }
    }

    private fun initRenderer(){

        val uri = Uri.parse(presentationData?.stringUri)

        try{
            val temp = File(this.cacheDir, "tempImage.pdf")
            val fos = FileOutputStream(temp)
            val ins: InputStream
            ins = if (presentationData?.debugFlag == 0) {
                val cr = contentResolver
                cr.openInputStream(uri)
            } else {
                assets.open(presentationData?.stringUri)
            }

            if (presentationData?.name!!.isNullOrEmpty())
                presentationName.setText(getFileName(uri,contentResolver))
            else
                presentationName.setText(presentationData?.name)


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
            Toast.makeText(this, "error in opening presentation file", Toast.LENGTH_LONG).show()
            Log.d("error","error in opening presentation file")
        }
    }
}