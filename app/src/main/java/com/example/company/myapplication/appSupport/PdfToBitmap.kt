package com.example.company.myapplication.appSupport

import android.annotation.SuppressLint
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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class PdfToBitmap{

    private var presentationStringUri: String?
    private var debugIntMode: Int?
    private val ctx: Context

    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null

    private var pageCount: Int? = null
    private var pageIndexStatus: Int? = null

    constructor(presentationUri: String, debugIntMode: Int, ctx: Context) {
        this.presentationStringUri = presentationUri
        this.debugIntMode = debugIntMode
        this.ctx = ctx

        initRenderer(presentationUri,debugIntMode)
    }

    constructor(ctx: Context) {
        this.ctx = ctx
        this.presentationStringUri = null
        this.debugIntMode = null
    }

    fun addPresentation(presentationUri: String, debugIntMode: Int) {
        this.debugIntMode = debugIntMode
        this.presentationStringUri = presentationUri

        initRenderer(presentationUri,debugIntMode)
    }

    private fun renderPage(pageIndex: Int): Bitmap? {
        currentPage?.close()
        currentPage = renderer?.openPage(pageIndex)
        val width = currentPage?.width
        val height = currentPage?.height
        val index = currentPage?.index
        val pageCount = renderer?.pageCount
        if(width != null && height != null && index != null && pageCount != null) {
            val nWidth: Int = width
            val nHeight: Int = height
            val bitmap: Bitmap = Bitmap.createBitmap(nWidth, nHeight, Bitmap.Config.ARGB_8888)
            currentPage?.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            pageIndexStatus = currentPage?.index
            return  bitmap
        }
        return null
    }
    private fun initRenderer(strUri: String, debugFlag: Int){
        val uri = Uri.parse(strUri)
        try{
            val temp = File(ctx.cacheDir, "tempImage.pdf")
            val fos = FileOutputStream(temp)
            val isChecked = debugFlag == 1
            val ins: InputStream
            ins = if(!isChecked) {
                try {
                    val cr = ctx.contentResolver
                    cr.openInputStream(uri)
                }catch (e: Exception) {
                    Log.d("test_row", "editPres cr:" + e.toString())
                } as InputStream
            } else {
                ctx.assets.open(strUri)
            }
            val buffer = ByteArray(1024)
            var readBytes = ins.read(buffer)
            while(readBytes != -1){
                fos.write(buffer, 0, readBytes)
                readBytes = ins.read(buffer)
            }
            fos.close()
            ins.close()
            parcelFileDescriptor = ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(parcelFileDescriptor)

            this.pageCount = renderer?.pageCount
        } catch(e: IOException){
            Toast.makeText(ctx, "error in opening presentation file", Toast.LENGTH_LONG).show()
            Log.d("error","error in opening presentation file")
        }
    }

    fun getBitmapForSlide(slideNumber: Int): Bitmap? {
        return renderPage(slideNumber)
    }

    fun saveSlideImage(fileName: String): Bitmap? {
        if (renderer == null || parcelFileDescriptor == null) {
            Toast.makeText(ctx, "Saving picture error", Toast.LENGTH_LONG).show()
            return  null
        }

        val temp = File(ctx.cacheDir, fileName)

        parcelFileDescriptor = ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_WRITE)
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

            return  bmpBase
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