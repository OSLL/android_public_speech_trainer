package ru.spb.speech.measurementAutomation

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.provider.DocumentFile
import android.util.Log
import android.widget.Toast
import ru.spb.speech.ACTIVITY_CREATE_PRESENTATION_NAME
import ru.spb.speech.APST_TAG
import ru.spb.speech.R
import ru.spb.speech.appSupport.PdfToBitmap
import ru.spb.speech.database.PresentationData
import ru.spb.speech.database.SpeechDataBase
import ru.spb.speech.database.helpers.PresentationDBHelper
import ru.spb.speech.database.interfaces.PresentationDataDao
import java.io.ByteArrayOutputStream
import java.io.File

class RunningTraining {

    companion object {
       const val LOG = "shit_log"
    }
    private val context: Context
    private val presentationDataDao: PresentationDataDao

    constructor(context: Context) {
        this.context = context
        presentationDataDao = SpeechDataBase.getInstance(context)?.PresentationDataDao()!!
        presentationDataDao.deleteTestFolderPres()


    }

    fun startTrainings(directoryFile: DocumentFile, timeClickList: Array<Int>){
        findPdfPresentations(directoryFile)
    }

    private fun findPdfPresentations(file: DocumentFile) {

        val files = file.listFiles()
        val timesList = mutableListOf<Long>()

        for(singleFile in files){
            Log.d(LOG, "file: $singleFile")
            if(singleFile.name.endsWith(".pdf")){
                addPres(singleFile.name, singleFile.uri.toString())
            } else if (singleFile.name.endsWith(".txt")) {
                try {

                } catch (e: Exception) {
                    e.printStackTrace()
                    presentationDataDao.deleteTestFolderPres()
                }
            }
        }
    }

    private var presentationData: PresentationData? = null
    private lateinit var pdfReader: PdfToBitmap

    private fun addPres(presName: String, path: String) {
        presentationData = PresentationData()
        presentationData!!.stringUri = path
        presentationData!!.debugFlag = 2
        presentationData!!.name = presName

        presentationDataDao.insert(presentationData!!)
        presentationData = presentationDataDao.getLastPresentation()

        pdfReader = PdfToBitmap(presentationData!!, context)
        presentationData?.apply {
            pageCount = pdfReader.getPageCount()
            timeLimit = context.resources.getInteger(R.integer.seconds_in_a_minute).toLong()
            presentationDate = context.getString(R.string.app_creation_date)
            notifications = false

        }
        presentationDataDao.updatePresentation(presentationData!!)
        val presF = PresentationDBHelper(context)
        presF.saveDefaultPresentationImageOnMainThread(presentationData!!.id!!)
    }
}