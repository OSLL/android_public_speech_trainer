package com.example.company.myapplication.DBTables.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.company.myapplication.appSupport.PdfToBitmap
import com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces.PresentationDataDao
import com.example.putkovdimi.trainspeech.DBTables.SpeechDataBase
import java.io.ByteArrayOutputStream

const val HELPER_LOG = "helper_log"

class PresentationDBHelper {
    private val presentationDataDao: PresentationDataDao
    private val pdfToBitmap: PdfToBitmap

    constructor(ctx: Context) {
        presentationDataDao = SpeechDataBase.getInstance(ctx)!!.PresentationDataDao()
        pdfToBitmap = PdfToBitmap(ctx)
    }

    fun changePresentationImage(presentationId: Int, image: Bitmap) {
        val presentation = presentationDataDao.getPresentationWithId(presentationId) ?: return
    }

    fun saveDefaultPresentationImage(presentationId: Int) {
        val presentation = presentationDataDao.getPresentationWithId(presentationId) ?: return
        pdfToBitmap.addPresentation(presentation.stringUri, presentation.debugFlag)
        val bm = pdfToBitmap.getBitmapForSlide(0) ?: return

        val stream = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.PNG, 20, stream)

        presentation.imageBLOB = stream.toByteArray()
        Log.d(HELPER_LOG, "before compress=${bm.byteCount}")
        Log.d(HELPER_LOG, "after compress=${BitmapFactory.decodeByteArray(presentation.imageBLOB, 0, stream.size()).byteCount}")
        presentationDataDao.updatePresentation(presentation)
        stream.close()
    }

    fun getPresentationImage(presentationId: Int): Bitmap? {
        try {
            val presentation = presentationDataDao.getPresentationWithId(presentationId)
            val blob = presentation?.imageBLOB

            val bm = BitmapFactory.decodeByteArray(blob, 0, blob!!.size)
            Log.d(HELPER_LOG, "get bitmap size:${bm.byteCount}")
            return bm
        } catch (e: Exception) {
            return null
        }
    }
}