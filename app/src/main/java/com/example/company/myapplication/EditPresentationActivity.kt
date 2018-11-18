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
import com.example.company.myapplication.appSupport.PdfToBitmap
import com.example.company.myapplication.views.PresentationStartpageRow
import com.example.company.myapplication.views.PresentationStartpageRow.Companion.activatedChangePresentationFlag
import com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces.PresentationDataDao
import com.example.putkovdimi.trainspeech.DBTables.PresentationData
import com.example.putkovdimi.trainspeech.DBTables.SpeechDataBase
import kotlinx.android.synthetic.main.activity_edit_presentation.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream



class EditPresentationActivity : AppCompatActivity() {

    private var presentationDataDao: PresentationDataDao? = null
    private var presentationData: PresentationData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_presentation)

        presentationDataDao = SpeechDataBase.getInstance(this)?.PresentationDataDao()
        val presId = intent.getIntExtra(getString(R.string.CURRENT_PRESENTATION_ID),-1)
        if (presId > 0) {
            presentationData = presentationDataDao?.getPresentationWithId(presId)
        }
        else {
            Log.d(APST_TAG, "edit_pres_act: wrong ID")
            return
        }

        val pdfReader = PdfToBitmap(presentationData!!.stringUri, presentationData!!.debugFlag, this)
        pdf_view.setImageBitmap(pdfReader.getBitmapForSlide(0))

        val changePresentationFlag = intent.getIntExtra(getString(R.string.changePresentationFlag), -1) == PresentationStartpageRow.activatedChangePresentationFlag
        if (changePresentationFlag)
            addPresentation.text = getString(R.string.further)

        if (presentationData?.name!!.isNullOrEmpty())
            presentationName.setText(getFileName(Uri.parse(presentationData!!.stringUri),contentResolver))
        else
            presentationName.setText(presentationData?.name)


        addPresentation.setOnClickListener{

            if (presentationName.text.toString() == ""){
                Toast.makeText(this, R.string.message_no_presentation_name, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(presentationName.text.length < 48) {
                val i = Intent(this, PresentationActivity::class.java)
                presentationData?.pageCount = pdfReader.getPageCount()
                presentationData?.name = presentationName.text.toString()
                presentationDataDao?.updatePresentation(presentationData!!)
              
                if (changePresentationFlag)
                i.putExtra(getString(R.string.changePresentationFlag), activatedChangePresentationFlag)

                i.putExtra(getString(R.string.CURRENT_PRESENTATION_ID), presentationData?.id)
                startActivity(i)
            }
            else
                Toast.makeText(this, R.string.pres_name_is_too_long, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
    }
}