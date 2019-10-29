package ru.spb.speech.appSupport

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import ru.spb.speech.ACTIVITY_CREATE_PRESENTATION_NAME
import ru.spb.speech.APST_TAG
import ru.spb.speech.R
import ru.spb.speech.database.PresentationData
import ru.spb.speech.database.SpeechDataBase
import ru.spb.speech.database.helpers.PresentationDBHelper
import ru.spb.speech.database.interfaces.PresentationDataDao
import java.io.ByteArrayOutputStream

class AddHelloPresentation (val context: Context) {

    private var speechDataBase: SpeechDataBase? = null
    private var presentationDataDao: PresentationDataDao? = null
    private var presentationData: PresentationData? = null
    private lateinit var pdfReader: PdfToBitmap
    private val defaultPictureSize: Int = 300
    private val defaultPictureQuality: Int = 100

    fun addPres() {

        presentationDataDao = SpeechDataBase.getInstance(context)?.PresentationDataDao()

        val presId = checkForPresentationInDB(context.getString(R.string.hello_presentation_file))

        if (presId != null && presId > 0) {
            presentationData = presentationDataDao?.getPresentationWithId(presId)
        } else {
            Log.d(APST_TAG, "edit_pres_act: wrong ID")
            return
        }

        pdfReader = PdfToBitmap(presentationData!!, context)
        presentationData?.pageCount = pdfReader.getPageCount()
        presentationData?.name = context.getString(R.string.hello_presentation_name)
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

                if (stringUri == context.getString(R.string.deb_pres_name) || stringUri == context.getString(R.string.hello_presentation_file))
                    newPresentation.debugFlag = 1
                else
                    context.contentResolver.takePersistableUriPermission(Uri.parse(stringUri), Intent.FLAG_GRANT_READ_URI_PERMISSION)

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