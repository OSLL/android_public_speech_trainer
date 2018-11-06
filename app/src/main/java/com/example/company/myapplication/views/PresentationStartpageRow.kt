package com.example.company.myapplication.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.company.myapplication.R
import com.example.putkovdimi.trainspeech.DBTables.PresentationData
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.presentation_startpage_row.view.*
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit
import android.graphics.BitmapFactory
import android.net.Uri
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat.startActivity
import android.util.Log
import android.widget.Toast
import com.example.company.myapplication.EditPresentationActivity
import com.example.company.myapplication.TrainingActivity
import com.example.company.myapplication.getFileName
import kotlinx.android.synthetic.main.activity_edit_presentation.*
import java.io.File
import java.io.FileOutputStream


class PresentationStartpageRow(private val presentation: PresentationData, private val ctx: Context): Item<ViewHolder>() {
    companion object {
        const val activatedChangePresentationFlag = 1
    }

    private var renderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    var presentationId: Int? = null

    override fun getLayout(): Int {
        return R.layout.presentation_startpage_row
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.name_presentation_start_page_row.text = presentation.name
        viewHolder.itemView.time_limit_presentation_start_page_row.text = getStringPresentationTimeLimit(presentation.timeLimit!!)
        viewHolder.itemView.page_count_presentation_start_page_row.text = getStringPresentationPageCount(presentation.pageCount)
        viewHolder.itemView.image_view_presentation_start_page_row.setImageBitmap(getFirstSlideBitmap(presentation.stringUri))

        presentationId = presentation.id

        viewHolder.itemView.change_btn_presentation_start_page_row.setOnClickListener {
            val i = Intent(ctx, EditPresentationActivity::class.java)
            i.putExtra(ctx.getString(R.string.CURRENT_PRESENTATION_ID),presentation.id)
            i.putExtra(ctx.getString(R.string.changePresentationFlag), activatedChangePresentationFlag)
            startActivity(ctx,i,null)
        }
    }

    @SuppressLint("UseSparseArrays")
    private fun getStringPresentationTimeLimit(t: Long?): String {
        if (t == null)
            return "undefined"

        var millisUntilFinishedVar: Long = t


        val minutes = TimeUnit.SECONDS.toMinutes(millisUntilFinishedVar)
        millisUntilFinishedVar -= TimeUnit.MINUTES.toSeconds(minutes)

        val seconds = millisUntilFinishedVar

        return String.format(
                Locale.getDefault(),
                "%02d min: %02d sec",
                minutes, seconds
        )
    }

    private fun getStringPresentationPageCount(n: Int?): String {
        if (n == null || n <= 0) {
            return "undefined"
        }

        val titles = arrayOf("$n слайд","$n слайда","$n слайдов")
        val cases = arrayOf(2, 0, 1, 1, 1, 2)

        return titles[if (n % 100 in 5..19) 2 else cases[if (n % 10 < 5) n % 10 else 5]]
    }

    private fun renderPage(pageIndex: Int): Bitmap? {

        currentPage?.close()
        currentPage = renderer?.openPage(pageIndex)
        val width = currentPage?.width
        val height = currentPage?.height
        val index = currentPage?.index
        val pageCount = renderer?.pageCount

        if(width != null && height != null && index != null && pageCount != null) {
            val NWidth: Int = width
            val NHeight: Int = height
            val bitmap: Bitmap = Bitmap.createBitmap(NWidth, NHeight, Bitmap.Config.ARGB_8888)
            currentPage?.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return  bitmap
        }
        return null
    }

    private fun initRenderer(strUri: String){
        val uri = Uri.parse(strUri)

        try{
            val temp = File(ctx.cacheDir, "tempImage.pdf")
            val fos = FileOutputStream(temp)
            val isChecked = presentation.debugFlag == 1
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
        } catch(e: IOException){
            Toast.makeText(ctx, "error in opening presentation file", Toast.LENGTH_LONG).show()
            Log.d("error","error in opening presentation file")
        }
    }

    private fun getFirstSlideBitmap(strUri: String): Bitmap? {
        initRenderer(strUri)
        return renderPage(0)
    }
}