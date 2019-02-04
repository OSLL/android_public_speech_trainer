package com.example.company.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import com.example.company.myapplication.views.PresentationStartpageItemRow
import com.example.company.myapplication.appSupport.PdfToBitmap
import com.example.company.myapplication.appSupport.ProgressHelper
import com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces.PresentationDataDao
import com.example.putkovdimi.trainspeech.DBTables.PresentationData
import com.example.putkovdimi.trainspeech.DBTables.SpeechDataBase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_start_page.*
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.NullPointerException


const val debugSlides = "making_presentation.pdf"   //Название презентации из ресурсов для отладочного режима
const val PageCount = 26       //Количество страниц в презентации, используемой для отладочного режима

const val debugSpeechAudio = R.raw.assembler // Путь к файлу в raw,
// который запускается в виде тестовой звуковой дорожки.

const val SHARED_PREFERENCES_FILE_NAME = "com.example.company.myapplication.prefs"

class StartPageActivity : AppCompatActivity() {
    companion object {
        var adapter: GroupAdapter<ViewHolder>? = null
    }

    private var listPresentationData: List<PresentationData>? = null
    private var presentationDataDao: PresentationDataDao? = null
    private lateinit var progressHelper: ProgressHelper

    private var pdfReader: PdfToBitmap? = null

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_page)

        progressHelper = ProgressHelper(this, start_page_root, listOf(recyclerview_startpage, addBtn))
        presentationDataDao = SpeechDataBase.getInstance(this)?.PresentationDataDao()

        if (!checkPermissions())
            checkPermissions()

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

    override fun onPause() {
        progressHelper.show()
        super.onPause()
    }

    override fun onResume() {
        progressHelper.hide()
        super.onResume()
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
            R.id.action_voice_analysis -> {
                val intent = Intent(this, VoiceAnalysisActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun refreshRecyclerView() {
        listPresentationData = presentationDataDao?.getAll()
        if (listPresentationData == null || presentationDataDao == null) {
            Toast.makeText(this, "fillRecError", Toast.LENGTH_LONG).show()
            return
        }

        if (adapter == null) {
            adapter = GroupAdapter<ViewHolder>()
            for (presentation in listPresentationData!!) {
                try {
                    if (presentation.timeLimit == null || presentation.pageCount == 0) {
                        presentationDataDao?.deletePresentationWithId(presentation.id!!)
                        continue
                    }
                    pdfReader = PdfToBitmap(presentation.stringUri, presentation.debugFlag, this)
                    adapter?.add(PresentationStartpageItemRow(presentation, pdfReader?.getBitmapForSlide(0), this@StartPageActivity))
                } catch (e: Exception) {
                    Toast.makeText(this, "file: ${presentation.stringUri} \nTYPE ERROR.\nDeleted from DB!", Toast.LENGTH_LONG).show()
                    presentationDataDao?.deletePresentationWithId(presentation.id!!)
                }
            }
            recyclerview_startpage.adapter = adapter
        }
        else {
            for (i in 0..(listPresentationData!!.size - 1)) {
                val presentation = listPresentationData!![i]
                pdfReader = PdfToBitmap(presentation.stringUri, presentation.debugFlag, this)

                if (presentation.timeLimit == null || presentation.pageCount == 0) {
                    presentationDataDao?.deletePresentationWithId(presentation.id!!)
                    continue
                }
                if (i > (adapter!!.itemCount - 1)) {
                    adapter?.add(PresentationStartpageItemRow(presentation, pdfReader?.getBitmapForSlide(0), this@StartPageActivity))
                    adapter?.notifyDataSetChanged()
                    recyclerview_startpage.adapter = adapter
                    continue
                }

                val row = adapter!!.getItem(i) as PresentationStartpageItemRow
                if (row.presentationTimeLimit != presentation.timeLimit || row.presentationName != presentation.name) {
                    adapter?.removeGroup(i)
                    adapter?.add(i, PresentationStartpageItemRow(presentation, pdfReader?.getBitmapForSlide(0), this@StartPageActivity))

                    adapter?.notifyDataSetChanged()
                }
                recyclerview_startpage.adapter = adapter
            }

        }

        recyclerview_startpage.isLongClickable = true

        adapter?.setOnItemClickListener{ item: Item<ViewHolder>, view: View ->
            //progressHelper.show()

            if (isOnline()) {
                val row = item as PresentationStartpageItemRow
                val i = Intent(this, TrainingActivity::class.java)
                i.putExtra(getString(R.string.CURRENT_PRESENTATION_ID), row.presentationId)
                startActivity(i)
            }
            else
                Toast.makeText(
                        applicationContext, R.string.no_internet_connection, Toast.LENGTH_SHORT
                ).show()
        }

        adapter?.setOnItemLongClickListener { item: Item<ViewHolder>, view ->
            val row = item as PresentationStartpageItemRow

            val defaultBackGround = view.background
            view.background = getDrawable(R.drawable.training_not_end_item_background)

            val builder = AlertDialog.Builder(this)
            builder.setMessage(getString(R.string.request_for_remove_presentation) + "${row.presentationName} ?")
            builder.setPositiveButton(getString(R.string.remove)) { _, _ ->
                val position = StartPageActivity.adapter?.getAdapterPosition(item)
                StartPageActivity.adapter?.remove(item)
                StartPageActivity.adapter?.notifyItemRemoved(position!!)
                recyclerview_startpage.adapter = adapter

                try {
                    recyclerview_startpage.scrollToPosition(position!!)
                } catch (e: NullPointerException) {

                }

                if (row.presentationId != null)
                    SpeechDataBase.getInstance(this)?.PresentationDataDao()?.deletePresentationWithId(row.presentationId!!)
                else {
                }
            }

            builder.setNegativeButton(getString(R.string.change)) { _, _ ->
                val i = Intent(this, EditPresentationActivity::class.java)
                i.putExtra(getString(R.string.CURRENT_PRESENTATION_ID),row.presentationId)
                i.putExtra(getString(R.string.changePresentationFlag), PresentationStartpageItemRow.activatedChangePresentationFlag)
                startActivity(i)
                view.background = defaultBackGround
            }
            builder.setOnCancelListener {
                view.background = defaultBackGround
            }
            val dialog: AlertDialog = builder.create()
            dialog.show()
            true
        }

    }

    private fun isOnline(): Boolean {
        var connection = false
        val thread = Thread(Runnable {
            connection = try {
                val socket = Socket()
                socket.connect(InetSocketAddress("8.8.8.8", 53), 1500)
                socket.close()
                true
            } catch (e: IOException) {
                false
            }
        })
        thread.start()
        thread.join()

        return connection
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
        runLayoutAnimation(recyclerview_startpage)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
    override fun onStop() {
        super.onStop()
    }

    private fun runLayoutAnimation(recyclerView: RecyclerView) {
        val context: Context = recyclerView.context
        val controller =
                AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down);
        recyclerView.layoutAnimation = controller
        try {
            recyclerView.adapter.notifyDataSetChanged()
        }catch (e: NullPointerException) {}
        recyclerView.scheduleLayoutAnimation()
    }
}