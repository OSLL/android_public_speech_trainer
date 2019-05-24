package ru.spb.speech.database.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import ru.spb.speech.appSupport.PdfToBitmap
import ru.spb.speech.R
import ru.spb.speech.database.interfaces.PresentationDataDao
import ru.spb.speech.database.SpeechDataBase
import java.io.ByteArrayOutputStream

class PresentationDBHelper {
    private val presentationDataDao: PresentationDataDao
    private val pdfToBitmap: PdfToBitmap
    private val defaultPictureSize: Int
    private val defaultPictureQuality: Int
    private val db: SpeechDataBase
    private val ctx: Context

    constructor(ctx: Context) {
        this.ctx = ctx
        db = SpeechDataBase.getInstance(ctx)!!
        presentationDataDao = db.PresentationDataDao()
        pdfToBitmap = PdfToBitmap(ctx)
        defaultPictureSize = ctx.resources.getInteger(R.integer.defaultPictureSize)
        defaultPictureQuality = ctx.resources.getInteger(R.integer.defaultPictureQuality)
    }

    fun changePresentationImage(presentationId: Int, image: Bitmap) {
        val presentation = presentationDataDao.getPresentationWithId(presentationId) ?: return
        val stream = ByteArrayOutputStream()
        getResizedBitmap(image, defaultPictureSize).compress(Bitmap.CompressFormat.PNG, defaultPictureQuality, stream)
        presentation.imageBLOB = stream.toByteArray()
        presentationDataDao.updatePresentation(presentation)
        stream.close()
    }

    fun removePresentation(id: Int) {
        val slidesHelper = TrainingSlideDBHelper(ctx)
        val pd = presentationDataDao.getPresentationWithId(id) ?: return
        val trainings = TrainingDBHelper(ctx).getAllTrainingsForPresentation(pd)
        db.PresentationDataDao().deletePresentationWithId(pd.id!!)
        if (trainings == null) return

        for (t in trainings) {
            for (s in slidesHelper.getAllSlidesForTraining(t) ?: continue)
                db.TrainingSlideDataDao().deleteSlideWithId(s.id!!)
            db.TrainingDataDao().deleteTrainingWithId(t.id!!)
        }
    }

    suspend fun saveDefaultPresentationImage(presentationId: Int) {
        val presentation = presentationDataDao.getPresentationWithId(presentationId) ?: return
        pdfToBitmap.addPresentation(presentation.stringUri, presentation.debugFlag)
        val bm = pdfToBitmap.getBitmapForSlide(0) ?: return
        val stream = ByteArrayOutputStream()
        getResizedBitmap(bm, defaultPictureSize).compress(Bitmap.CompressFormat.PNG, defaultPictureQuality, stream)
        presentation.imageBLOB = stream.toByteArray()
        presentationDataDao.updatePresentation(presentation)
        stream.close()
    }

    fun getPresentationImage(presentationId: Int): Bitmap? {
        return try {
            val presentation = presentationDataDao.getPresentationWithId(presentationId)
            val blob = presentation?.imageBLOB
            val bm = BitmapFactory.decodeByteArray(blob, 0, blob!!.size)
            bm
        } catch (e: Exception) { null }
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