package com.example.company.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_start_page.*
import android.widget.Toast
import com.example.company.myapplication.views.PresentationStartpageRow
import com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces.PresentationDataDao
import com.example.putkovdimi.trainspeech.DBTables.PresentationData
import com.example.putkovdimi.trainspeech.DBTables.SpeechDataBase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.OnItemClickListener
import com.xwray.groupie.ViewHolder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


const val debugSlides = "making_presentation.pdf"   //Название презентации из ресурсов для отладочного режима
const val PageCount = 26       //Количество страниц в презентации, используемой для отладочного режима

const val debugSpeechAudio = R.raw.assembler // Путь к файлу в raw,
                                                  // который запускается в виде тестовой звуковой дорожки.

const val SHARED_PREFERENCES_FILE_NAME = "com.example.company.myapplication.prefs"

class StartPageActivity : AppCompatActivity() {
    companion object {
        const val actionInformation = "actionInformation"
        const val TRAINING_FLAG = -1
        const val CHANGE_PRESENTATION_FLAG = -2
    }

    private var adapter: GroupAdapter<ViewHolder>? = null
    private var listPresentationData: List<PresentationData>? = null
    private var presentationDataDao: PresentationDataDao? = null

    private var renderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_page)

        if (!checkPermissions())
            checkPermissions()

        adapter = GroupAdapter<ViewHolder>()

        val sharedPref = getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            if (sharedPref.contains(getString(R.string.audio_recording))) {
                return@with
            }
            putBoolean(getString(R.string.audio_recording), true)
            apply()
        }


        addBtn.setOnClickListener{
            val intent = Intent(this, CreatePresentationActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item?.itemId
        when (id) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun fillRecyclerView() {
        presentationDataDao = SpeechDataBase.getInstance(this)?.PresentationDataDao()
        listPresentationData = presentationDataDao?.getAll()

        if (listPresentationData == null || adapter == null || presentationDataDao == null) {
            Toast.makeText(this, "fillRecError",Toast.LENGTH_LONG).show()
            return
        }

        for (presentation in listPresentationData!!) {
            try {
                if (presentation.timeLimit == null || presentation.pageCount == 0) {
                    presentationDataDao?.deletePresentationWithId(presentation.id!!)
                    continue
                }

                adapter?.add(PresentationStartpageRow(presentation, getFirstSlideBitmap(presentation.stringUri, presentation.debugFlag), this@StartPageActivity))
            } catch (e: Exception) {
                Toast.makeText(this,"file: ${presentation.stringUri} \nTYPE ERROR.\nDeleted from DB!",Toast.LENGTH_LONG).show()
                presentationDataDao?.deletePresentationWithId(presentation.id!!)

            }
        }

        recyclerview_startpage.adapter = adapter

        adapter?.setOnItemClickListener{ item: Item<ViewHolder>, view: View ->
            val row = item as PresentationStartpageRow
            val i = Intent(this, TrainingActivity::class.java)
            i.putExtra(getString(R.string.CURRENT_PRESENTATION_ID), row.presentationId)
            startActivity(i)
        }
    }

    private fun refreshRecyclerView() {
        adapter?.clear()
        adapter?.notifyDataSetChanged()
        fillRecyclerView()
    }

    fun checkPermissions(): Boolean {
        val permissions = ArrayList<String>()
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        permissions.add(Manifest.permission.INTERNET)
        permissions.add(Manifest.permission.RECORD_AUDIO)

        var result: Int
        val listPermissionsNeeded = ArrayList<String>()
        for (p in permissions) {
            result = ContextCompat.checkSelfPermission(this, p)
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p)
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toTypedArray(), 100)
            return false
        }
        return true
    }

    override fun onStart() {
        super.onStart()
        refreshRecyclerView()
    }

    override fun onStop() {
        super.onStop()
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

    private fun initRenderer(strUri: String, debugFlag: Int){
        val uri = Uri.parse(strUri)

        try{
            val temp = File(cacheDir, "tempImage.pdf")
            val fos = FileOutputStream(temp)
            val isChecked = debugFlag == 1
            val ins: InputStream
            ins = if(!isChecked) {
                try {
                    val cr = contentResolver
                    cr.openInputStream(uri)
                }catch (e: Exception) {
                    Log.d("test_row", "editPres cr:" + e.toString())
                } as InputStream
            } else {
                assets.open(strUri)
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
            Toast.makeText(this, "error in opening presentation file", Toast.LENGTH_LONG).show()
            Log.d("error","error in opening presentation file")
        }
    }

    private fun getFirstSlideBitmap(strUri: String, debugFlag: Int): Bitmap? {
        initRenderer(strUri,debugFlag)
        return renderPage(0)
    }
}
