package com.example.company.myapplication

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import com.example.company.myapplication.views.PresentationStartpageItemRow
import com.example.company.myapplication.appSupport.PdfToBitmap
import com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces.PresentationDataDao
import com.example.putkovdimi.trainspeech.DBTables.PresentationData
import com.example.putkovdimi.trainspeech.DBTables.SpeechDataBase
import kotlinx.android.synthetic.main.activity_edit_presentation.*
import android.widget.NumberPicker
import com.example.company.myapplication.appSupport.ProgressHelper

class EditPresentationActivity : AppCompatActivity() {

    private var presentationDataDao: PresentationDataDao? = null
    private var presentationData: PresentationData? = null
    private lateinit var progressHelper: ProgressHelper

    private var formatter: NumberPicker.Formatter = NumberPicker.Formatter { value ->
        val temp = value * 10
        "" + temp
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_presentation)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        progressHelper = ProgressHelper(this, edit_presentation_activity_root, listOf(addPresentation, numberPicker1, presentationName))

        numberPicker1.maxValue = 100
        numberPicker1.minValue = 1

        try {
            presentationDataDao = SpeechDataBase.getInstance(this)?.PresentationDataDao()
            val presId = intent.getIntExtra(getString(R.string.CURRENT_PRESENTATION_ID), -1)
            if (presId > 0) {
                presentationData = presentationDataDao?.getPresentationWithId(presId)
            } else {
                Log.d(APST_TAG, "edit_pres_act: wrong ID")
                return
            }

            val changePresentationFlag = intent.getIntExtra(getString(R.string.changePresentationFlag), -1) == PresentationStartpageItemRow.activatedChangePresentationFlag

            val pdfReader = PdfToBitmap(presentationData!!.stringUri, presentationData!!.debugFlag, this)
            pdf_view.setImageBitmap(pdfReader.getBitmapForSlide(0))

            Log.d("dfvgbh", presentationData?.timeLimit.toString())
            if (changePresentationFlag) {
                title = getString(R.string.presentationEditing)

                numberPicker1.value = (presentationData?.timeLimit!! / 60).toInt()
            } else {
                val defTime = pdfReader.getPageCount()
                if (defTime == null || defTime < 1) {
                    Toast.makeText(this, "page count error", Toast.LENGTH_LONG).show()
                    finish()
                    return
                }

                if (defTime > 100) numberPicker1.value = 100
                else numberPicker1.value = defTime
            }

            if (presentationData?.name!!.isNullOrEmpty())
                presentationName.setText(getFileName(Uri.parse(presentationData!!.stringUri), contentResolver))
            else
                presentationName.setText(presentationData?.name)


            addPresentation.setOnClickListener {

                if (presentationName.text.toString() == "") {
                    Toast.makeText(this, R.string.message_no_presentation_name, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (presentationName.text.length < 48) {
                    presentationData?.pageCount = pdfReader.getPageCount()
                    presentationData?.name = presentationName.text.toString()
                    presentationData?.timeLimit = numberPicker1.value.toLong() * 60L
                    presentationDataDao?.updatePresentation(presentationData!!)

                    finish()
                } else
                    Toast.makeText(this, R.string.pres_name_is_too_long, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            super.onBackPressed()
            return true
        }
        return false
    }

    override fun onPause() {
        progressHelper.show()
        super.onPause()
    }

    override fun onResume() {
        progressHelper.hide()
        super.onResume()
    }

    override fun onStart() {
        super.onStart()
    }
}