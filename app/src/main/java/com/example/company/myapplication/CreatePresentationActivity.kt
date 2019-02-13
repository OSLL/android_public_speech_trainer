package com.example.company.myapplication

import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.example.putkovdimi.trainspeech.DBTables.PresentationData
import com.example.putkovdimi.trainspeech.DBTables.SpeechDataBase
import java.io.FileNotFoundException

const val FILE_SYSTEM = "file_system"
const val APST_TAG = "APST"
const val ACTIVITY_CREATE_PRESENTATION_NAME = ".CreatePresentationActivity"

class CreatePresentationActivity : AppCompatActivity() {
    private var speechDataBase: SpeechDataBase? = null

    private val REQUSETCODE = 111

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isChecked = sharedPreferences.getBoolean(getString(R.string.deb_pres), false)
        super.onCreate(savedInstanceState)
        if (!isChecked) {
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
            val myUri = Uri.parse(path)
            val intent = Intent(ACTION_OPEN_DOCUMENT)
                    .setDataAndType(myUri, "*/*")
                    .addCategory(CATEGORY_OPENABLE)
            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_a_file)), REQUSETCODE)
        } else {
            val i = Intent(this, EditPresentationActivity::class.java)
            i.putExtra(getString(R.string.CURRENT_PRESENTATION_ID), checkForPresentationInDB(getString(R.string.deb_pres_name)))
            startActivity(i)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUSETCODE && resultCode == RESULT_OK && data != null) {
            val selectedFile = data.data //The uri with the location of the file
            try {
                val i = Intent(this, EditPresentationActivity::class.java)
                val dbPresentationId = checkForPresentationInDB(selectedFile.toString())
                i.putExtra(getString(R.string.CURRENT_PRESENTATION_ID), dbPresentationId)
                Log.d(FILE_SYSTEM, selectedFile.toString())
                startActivity(i)
            } catch (e: FileNotFoundException) {
                Log.d(FILE_SYSTEM, "file not found")
            }
        }

        finish()
    }

    private fun checkForPresentationInDB(stringUri: String): Int? {
        try {
            speechDataBase = SpeechDataBase.getInstance(this)

            var newPresentation: PresentationData? = speechDataBase?.PresentationDataDao()?.getPresentationDataWithUri(stringUri)
            var currentPresID: Int? = newPresentation?.id

            if (newPresentation == null) {
                newPresentation = PresentationData()
                newPresentation.stringUri = stringUri

                if (stringUri == getString(R.string.deb_pres_name))
                    newPresentation.debugFlag = 1

                speechDataBase?.PresentationDataDao()?.insert(newPresentation)
                currentPresID = speechDataBase?.PresentationDataDao()?.getPresentationDataWithUri(stringUri)?.id

                Log.d(APST_TAG + ACTIVITY_CREATE_PRESENTATION_NAME, "create new pres: $newPresentation")
            } else {
                Log.d(APST_TAG + ACTIVITY_CREATE_PRESENTATION_NAME, "open exists presentation: $newPresentation")
                Toast.makeText(this, "This presentation has already been added!", Toast.LENGTH_LONG).show()
            }

            return currentPresID
        } catch (e: Exception) {
            return null
        }
    }
}

