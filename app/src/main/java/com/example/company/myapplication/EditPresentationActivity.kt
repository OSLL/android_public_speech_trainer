package com.example.company.myapplication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.example.company.myapplication.DBTables.helpers.PresentationDBHelper
import com.example.company.myapplication.views.PresentationStartpageItemRow
import com.example.company.myapplication.appSupport.PdfToBitmap
import com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces.PresentationDataDao
import com.example.putkovdimi.trainspeech.DBTables.PresentationData
import com.example.putkovdimi.trainspeech.DBTables.SpeechDataBase
import kotlinx.android.synthetic.main.activity_edit_presentation.*
import com.example.company.myapplication.appSupport.ProgressHelper
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.IOException

const val maximumPresentationLength = 48
const val secondsInAMinute = 60
const val valueWhenMissedIntent = -1

class EditPresentationActivity : AppCompatActivity() {

    private var presentationDataDao: PresentationDataDao? = null
    private var presentationData: PresentationData? = null
    private lateinit var progressHelper: ProgressHelper
    private lateinit var pdfReader: PdfToBitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_presentation)

        progressHelper = ProgressHelper(this, edit_presentation_activity_root, listOf(addPresentation, numberPicker1, presentationName))

        numberPicker1.maxValue = 100
        numberPicker1.minValue = 1

        try {
            presentationDataDao = SpeechDataBase.getInstance(this)?.PresentationDataDao()
            val presId = intent.getIntExtra(getString(R.string.CURRENT_PRESENTATION_ID), valueWhenMissedIntent)
            if (presId > 0) {
                presentationData = presentationDataDao?.getPresentationWithId(presId)
            } else {
                Log.d(APST_TAG, "edit_pres_act: wrong ID")
                return
            }
            val presentationUri = presentationData?.stringUri
            val changePresentationFlag = intent.getIntExtra(getString(R.string.changePresentationFlag), valueWhenMissedIntent) == PresentationStartpageItemRow.activatedChangePresentationFlag

            pdfReader = PdfToBitmap(presentationData!!, this)
            pdf_view.setImageBitmap(pdfReader.getBitmapForSlide(0))

            if (changePresentationFlag) {
                title = getString(R.string.presentationEditing)

                numberPicker1.value = (presentationData?.timeLimit!! / secondsInAMinute).toInt()
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

                if (presentationName.text.length > maximumPresentationLength) {
                    Toast.makeText(this, R.string.pres_name_is_too_long, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val isChanged = presentationName.text.toString() != presentationData?.name ||
                        numberPicker1.value.toLong() * secondsInAMinute.toLong() != presentationData?.timeLimit || presentationUri != presentationData?.stringUri

                presentationData?.pageCount = pdfReader.getPageCount()
                presentationData?.name = presentationName.text.toString()
                presentationData?.timeLimit = numberPicker1.value.toLong() * secondsInAMinute.toLong()
                presentationDataDao?.updatePresentation(presentationData!!)

                if (changePresentationFlag) {
                    val i = Intent()
                    i.putExtra(getString(R.string.isPresentationChangedFlag), isChanged)
                    i.putExtra(getString(R.string.presentationPosition),
                            intent.getIntExtra(getString(R.string.presentationPosition), valueWhenMissedIntent))
                    setResult(Activity.RESULT_OK, i)
                }

                this@EditPresentationActivity.onPause()
                GlobalScope.launch {
                    try {
                        val presentationDBHelper = async(IO) { PresentationDBHelper(this@EditPresentationActivity) }
                        presentationDBHelper.await().saveDefaultPresentationImage(presentationData?.id!!)
                        finish()
                    } catch (e: Exception) {
                        Log.d(APST_TAG, e.toString())
                        finish()
                    }
                }
            }
        } catch (e: Exception) {
            finish()
        }

        // у тестовой презентации не предусмотрена замена файла
        if (presentationData?.debugFlag == 1) change_pres.visibility = View.GONE
        change_pres.setOnClickListener {
            val i = Intent(this, CreatePresentationActivity::class.java)
            i.putExtra(getString(R.string.CHANGE_FILE_FLAG), true)
            startActivityForResult(i, resources.getInteger(R.integer.createPresentationActRequestCode))
            overridePendingTransition(0, 0)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            super.onBackPressed()
            return true
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == resources.getInteger(R.integer.createPresentationActRequestCode) && resultCode == RESULT_OK && data != null) {
            val uri = data.getStringExtra(getString(R.string.NEW_PRESENTATION_URI))

            try {
                pdfReader.addPresentation(uri, 0)
            } catch (e: IOException) {
                Toast.makeText(this, getString(R.string.pdfErrorMsg), Toast.LENGTH_LONG).show()
                return
            }

            pdf_view.setImageBitmap(pdfReader.getBitmapForSlide(0))
            presentationData?.stringUri = uri
        }
    }

    override fun onPause() {
        try {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            progressHelper.show()
        } catch (e: Exception) {
        }
        super.onPause()
    }

    override fun onResume() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        progressHelper.hide()
        super.onResume()
    }

    override fun onStart() {
        super.onStart()
    }
}