package ru.spb.speech

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import ru.spb.speech.appSupport.ProgressHelper
import ru.spb.speech.DBTables.DaoInterfaces.PresentationDataDao
import ru.spb.speech.DBTables.SpeechDataBase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_start_page.*
import ru.spb.speech.appSupport.appSupport.PresentationAdapterHelper
import ru.spb.speech.appSupport.appSupport.UpdateAdapterListener

const val debugSpeechAudio = R.raw.assembler // Путь к файлу в raw,
// который запускается в виде тестовой звуковой дорожки.

const val SHARED_PREFERENCES_FILE_NAME = "ru.spb.speech.prefs"

class StartPageActivity : AppCompatActivity(), UpdateAdapterListener {

    private lateinit var adapter: GroupAdapter<ViewHolder>
    private lateinit var presentationDataDao: PresentationDataDao
    private lateinit var progressHelper: ProgressHelper
    private lateinit var presentationAdapterHelper: PresentationAdapterHelper
    private  var currentPresentationsCount = 0

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_page)

        supportActionBar?.title = getString(R.string.activity_start_page_name)

        progressHelper = ProgressHelper(this, start_page_root, listOf(recyclerview_startpage, addBtn))
        presentationDataDao = SpeechDataBase.getInstance(this)!!.PresentationDataDao()

        adapter = GroupAdapter()
        recyclerview_startpage.adapter = adapter
        presentationAdapterHelper = PresentationAdapterHelper(recyclerview_startpage, adapter, this)
        presentationAdapterHelper.setUpdateAdapterListener(this)
        presentationAdapterHelper.fillAdapter()
        currentPresentationsCount = presentationDataDao.getAll().size

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
            R.id.about -> startActivity(Intent(this, AboutActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
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

        val count = presentationDataDao.getAll().size
        if (count > currentPresentationsCount) {
            currentPresentationsCount = count
            presentationAdapterHelper.addLastItem()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == resources.getInteger(R.integer.editPresentationResultCode)) {
            val flag = data?.getBooleanExtra(getString(R.string.isPresentationChangedFlag), true) ?: return
            if (!flag) return

            val position = data.getIntExtra(getString(R.string.presentationPosition), -1)
            if (position < 0)return

            presentationAdapterHelper.notifyItemChanged(position)
        }
    }

    override fun onAdapterUpdate() {
        this.currentPresentationsCount = presentationDataDao.getAll().size
    }
}