package ru.spb.speech.appSupport

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import ru.spb.speech.APST_TAG
import ru.spb.speech.R
import ru.spb.speech.DBTables.PresentationData
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class PdfToBitmap {
    private var presentationStringUri: String?
    private var debugIntMode: Int?
    private val ctx: Context

    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null

    private var pageCount: Int? = null
    private var pageIndexStatus: Int? = null

    constructor(presentation: PresentationData, ctx: Context) {
        this.presentationStringUri = presentation.stringUri
        this.debugIntMode = presentation.debugFlag
        this.ctx = ctx

        initRenderer()
    }

    constructor(ctx: Context) {
        this.ctx = ctx
        this.presentationStringUri = null
        this.debugIntMode = null
    }

    fun addPresentation(presentationUri: String, debugIntMode: Int) {
        this.debugIntMode = debugIntMode
        this.presentationStringUri = presentationUri

        initRenderer()
    }

    private fun renderPage(pageIndex: Int): Bitmap? {
        currentPage?.close()
        currentPage = renderer?.openPage(pageIndex)
        val width = currentPage?.width
        val height = currentPage?.height
        val index = currentPage?.index
        val pageCount = renderer?.pageCount
        if (width != null && height != null && index != null && pageCount != null) {
            val nWidth: Int = width
            val nHeight: Int = height
            val bitmap: Bitmap = Bitmap.createBitmap(nWidth, nHeight, Bitmap.Config.ARGB_8888)
            currentPage?.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            pageIndexStatus = currentPage?.index
            return bitmap
        }
        return null
    }

    private fun initRenderer() {
        val uri = Uri.parse(this.presentationStringUri)

        val temp = File(ctx.cacheDir, ctx.getString(R.string.tempImageName))
        val outPutStream = FileOutputStream(temp)
        val isChecked = this.debugIntMode == 1
        val inputStream: InputStream
        inputStream = if (!isChecked) {
            try {
                val cr = ctx.contentResolver
                cr.openInputStream(uri)
            } catch (e: Exception) {
                Log.d(APST_TAG + PdfToBitmap::class.toString(), e.toString())
            } as InputStream
        } else {
            ctx.assets.open(this.presentationStringUri)
        }
        val buffer = ByteArray(1024)
        var readBytes = inputStream.read(buffer)
        while (readBytes != -1) {
            outPutStream.write(buffer, 0, readBytes)
            readBytes = inputStream.read(buffer)
        }
        outPutStream.close()
        inputStream.close()
        parcelFileDescriptor = ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(parcelFileDescriptor)

        this.pageCount = renderer?.pageCount

    }

    fun getBitmapForSlide(slideNumber: Int): Bitmap? {
        return renderPage(slideNumber)
    }

    fun saveSlideImage(fileName: String): Bitmap? {
        if (renderer == null || parcelFileDescriptor == null) {
            Toast.makeText(ctx, "Saving picture error", Toast.LENGTH_LONG).show()
            return null
        }

        val temp = File(ctx.cacheDir, fileName)

        parcelFileDescriptor = ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(parcelFileDescriptor)

        currentPage = renderer?.openPage(0)
        val width = currentPage?.width
        val height = currentPage?.height
        if (width != null && height != null) {
            val defW = 397f
            val defH = 298f

            val coeff = height.toFloat() / width.toFloat()

            val curW = defW.toInt()
            val curH = (defW * coeff).toInt()

            val bmpBase = Bitmap.createBitmap(curW, curH, Bitmap.Config.ARGB_8888)

            val whitePaint = Paint()
            whitePaint.style = Paint.Style.FILL
            whitePaint.color = Color.WHITE

            val bmpToWhite = Canvas(bmpBase)
            bmpToWhite.drawPaint(whitePaint)

            currentPage?.render(bmpBase, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

            return bmpBase
        }
        return null
    }

    fun getPageCount(): Int? {
        return pageCount
    }

    fun getPageIndexStatus(): Int? {
        return pageIndexStatus
    }

    fun finish() {
        currentPage?.close()
        try {
            parcelFileDescriptor?.close()
        } catch (e: IOException) {
            Toast.makeText(ctx, "error in closing FileDescriptor", Toast.LENGTH_LONG).show()
        }
        renderer?.close()
    }
}