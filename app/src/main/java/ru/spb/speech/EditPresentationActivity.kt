package ru.spb.speech

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import ru.spb.speech.views.PresentationStartpageItemRow
import ru.spb.speech.appSupport.PdfToBitmap
import ru.spb.speech.DBTables.DaoInterfaces.PresentationDataDao
import ru.spb.speech.DBTables.PresentationData
import ru.spb.speech.DBTables.SpeechDataBase
import ru.spb.speech.DBTables.DBTables.helpers.PresentationDBHelper
import ru.spb.speech.appSupport.ProgressHelper
import kotlinx.android.synthetic.main.activity_edit_presentation.*
import java.util.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.IOException

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

        progressHelper = ProgressHelper(this, edit_presentation_activity_root, listOf(addPresentation, numberPicker1, presentationName, datePicker))

        numberPicker1.maxValue = 100
        numberPicker1.minValue = 1

        val calendar = Calendar.getInstance()
        datePicker.minDate = calendar.timeInMillis
        datePicker.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH) + 7)

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

            val date = presentationData?.presentationDate

            if (changePresentationFlag) {
                title = getString(R.string.presentationEditing)

                numberPicker1.value = (presentationData?.timeLimit!! / 60).toInt()

                val dateArr = date!!.split("-").map { it.toInt() }
                //[0] - day, [1] - month, [2] - year
                datePicker.updateDate(dateArr[0],dateArr[1] - 1, dateArr[2])
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

                val isChanged = presentationName.text.toString() != presentationData?.name ||
                        numberPicker1.value.toLong() * 60L != presentationData?.timeLimit ||
                        presentationData?.presentationDate != "${datePicker.year}-${datePicker.month + 1}-${datePicker.dayOfMonth}" ||
                        presentationUri != presentationData?.stringUri

                presentationData?.pageCount = pdfReader.getPageCount()
                presentationData?.name = presentationName.text.toString()
                presentationData?.timeLimit = numberPicker1.value.toLong() * 60L
                presentationData?.presentationDate = "${datePicker.year}-${datePicker.month + 1}-${datePicker.dayOfMonth}"

                presentationDataDao?.updatePresentation(presentationData!!)

                if (changePresentationFlag) {
                    val i = Intent()
                    i.putExtra(getString(R.string.isPresentationChangedFlag), isChanged)
                    i.putExtra(getString(R.string.presentationPosition),
                            intent.getIntExtra(getString(R.string.presentationPosition), valueWhenMissedIntent))
                    setResult(Activity.RESULT_OK, i)

                    if (presentationUri == presentationData?.stringUri) {
                        finish()
                        return@setOnClickListener
                    }
                }

                this@EditPresentationActivity.onPause()
                GlobalScope.launch {
                    try {
                        val presentationDBHelper = async(IO) { PresentationDBHelper(this@EditPresentationActivity) }
                        presentationDBHelper.await().saveDefaultPresentationImage(presentationData?.id!!)
                    } catch (e: Exception) {
                        Log.d(APST_TAG, e.toString())
                    }
                    finish()
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
            Log.d(APST_TAG, e.toString())
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