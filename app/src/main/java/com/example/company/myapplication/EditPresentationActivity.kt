package com.example.company.myapplication

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
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
import com.example.company.myapplication.appSupport.ProgressHelper
import java.io.ByteArrayOutputStream

class EditPresentationActivity : AppCompatActivity() {

    private var presentationDataDao: PresentationDataDao? = null
    private var presentationData: PresentationData? = null
    private lateinit var progressHelper: ProgressHelper

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

            val pdfReader = PdfToBitmap(presentationData!!, this)
            pdf_view.setImageBitmap(pdfReader.getBitmapForSlide(0))

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

                if (presentationName.text.length > 48) {
                    Toast.makeText(this, R.string.pres_name_is_too_long, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val isChanged = presentationName.text.toString() != presentationData?.name ||
                        numberPicker1.value.toLong() * 60L != presentationData?.timeLimit

                presentationData?.pageCount = pdfReader.getPageCount()
                presentationData?.name = presentationName.text.toString()
                presentationData?.timeLimit = numberPicker1.value.toLong() * 60L
                presentationDataDao?.updatePresentation(presentationData!!)

                if (changePresentationFlag) {
                    val i = Intent()
                    i.putExtra(getString(R.string.isPresentationChangedFlag), isChanged)
                    i.putExtra(getString(R.string.presentationPosition),
                            intent.getIntExtra(getString(R.string.presentationPosition), -1))
                    setResult(Activity.RESULT_OK, i)
                }

                // finish in async
                SaveDefaultPictureAsync(presentationData!!).execute()
            }
        } catch (e: Exception) { finish() }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            super.onBackPressed()
            return true
        }
        return false
    }

    override fun onPause() {
        try {
            progressHelper.show()
        } catch (e: Exception) {}
        super.onPause()
    }

    override fun onResume() {
        progressHelper.hide()
        super.onResume()
    }

    override fun onStart() {
        super.onStart()
    }

    private inner class SaveDefaultPictureAsync(private val presentation: PresentationData)
        : AsyncTask<Void, Void, Void>() {
        private lateinit var stream: ByteArrayOutputStream
        private var bm: Bitmap? = null
        private lateinit var pdfToBitmap: PdfToBitmap

        override fun onPreExecute() {
            this@EditPresentationActivity.onPause()
            super.onPreExecute()
            stream = ByteArrayOutputStream()
            pdfToBitmap = PdfToBitmap(presentation, this@EditPresentationActivity)
        }

        override fun doInBackground(vararg params: Void?): Void? {
            try {
                bm = pdfToBitmap.getBitmapForSlide(0) ?: return null
                getResizedBitmap(bm!!, 300).compress(Bitmap.CompressFormat.PNG, 100, stream)
                publishProgress()
            } catch (e: Exception) { }
            return null
        }

        override fun onProgressUpdate(vararg values: Void?) {
            super.onProgressUpdate(*values)
            if (bm == null) return
            presentation.imageBLOB = stream.toByteArray()
            presentationDataDao?.updatePresentation(presentation)
            stream.close()
            this@EditPresentationActivity.finish()
        }

        private fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap {
            var width = image.width
            var height = image.height

            val bitmapRatio = width.toFloat() / height.toFloat()
            if (bitmapRatio > 1) {
                width = maxSize
                height = (width / bitmapRatio).toInt()
            } else {
                height = maxSize
                width = (height * bitmapRatio).toInt()
            }

            return Bitmap.createScaledBitmap(image, width, height, true)
        }
    }
}