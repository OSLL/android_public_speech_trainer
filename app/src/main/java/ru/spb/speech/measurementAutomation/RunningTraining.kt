package ru.spb.speech.measurementAutomation

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import ru.spb.speech.ACTIVITY_CREATE_PRESENTATION_NAME
import ru.spb.speech.APST_TAG
import ru.spb.speech.R
import ru.spb.speech.appSupport.PdfToBitmap
import ru.spb.speech.database.PresentationData
import ru.spb.speech.database.SpeechDataBase
import ru.spb.speech.database.interfaces.PresentationDataDao
import java.io.ByteArrayOutputStream
import java.io.File

class RunningTraining (val context: Context) {

    fun startTrainings(directoryUri: String, timeClickList: Array<Int>){

        val presArray = findPdfPresentations(File(directoryUri))

        for(pres in presArray) {
            addPres(pres.toString())
        }

    }

    private fun findPdfPresentations(file: File): ArrayList<File> {

        val presentationList = ArrayList<File>()
        val files = file.listFiles()

        for(singleFile in files){
            if(singleFile.name.endsWith(".pdf")){
                presentationList.add(singleFile)
            }
        }
        return presentationList
    }

    private var speechDataBase: SpeechDataBase? = null
    private var presentationDataDao: PresentationDataDao? = null
    private var presentationData: PresentationData? = null
    private lateinit var pdfReader: PdfToBitmap
    private val defaultPictureSize: Int = 300
    private val defaultPictureQuality: Int = 100

    private fun addPres(presName: String) {

        presentationDataDao = SpeechDataBase.getInstance(context)?.PresentationDataDao()

        val presId = checkForPresentationInDB(presName)

        if (presId != null && presId > 0) {
            presentationData = presentationDataDao?.getPresentationWithId(presId)
        } else {
            Log.d(APST_TAG, "edit_pres_act: wrong ID")
            return
        }

        pdfReader = PdfToBitmap(presentationData!!, context)
        presentationData?.pageCount = pdfReader.getPageCount()
        presentationData?.name = presName
        presentationData?.timeLimit = context.resources.getInteger(R.integer.seconds_in_a_minute).toLong()
        presentationData?.presentationDate = context.getString(R.string.app_creation_date)
        presentationData?.notifications = false
        presentationDataDao?.updatePresentation(presentationData!!)

        val presentation = presentationDataDao?.getPresentationWithId(presentationData?.id!!) ?: return
        pdfReader.addPresentation(presentation.stringUri, presentation.debugFlag)
        val bm = pdfReader.getBitmapForSlide(0) ?: return
        val stream = ByteArrayOutputStream()
        getResizedBitmap(bm, defaultPictureSize).compress(Bitmap.CompressFormat.PNG, defaultPictureQuality, stream)
        presentation.imageBLOB = stream.toByteArray()
        presentationDataDao?.updatePresentation(presentation)
        stream.close()
    }

    private fun checkForPresentationInDB(stringUri: String): Int? {
        try {
            speechDataBase = SpeechDataBase.getInstance(context)

            var newPresentation: PresentationData? = speechDataBase?.PresentationDataDao()?.getPresentationDataWithUri(stringUri)
            var currentPresID: Int? = newPresentation?.id

            if (newPresentation == null) {
                newPresentation = PresentationData()
                newPresentation.stringUri = stringUri

                newPresentation.debugFlag = 1

                speechDataBase?.PresentationDataDao()?.insert(newPresentation)
                currentPresID = speechDataBase?.PresentationDataDao()?.getPresentationDataWithUri(stringUri)?.id

                Log.d(APST_TAG + ACTIVITY_CREATE_PRESENTATION_NAME, "create new pres: $newPresentation")
            } else {
                Log.d(APST_TAG + ACTIVITY_CREATE_PRESENTATION_NAME, "open exists presentation: $newPresentation")
                Toast.makeText(context, "This presentation has already been added!", Toast.LENGTH_LONG).show()
            }

            return currentPresID
        } catch (e: Exception) {
            return null
        }
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