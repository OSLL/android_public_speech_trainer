package com.example.company.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_start_page.*


const val debugSlides = "making_presentation.pdf"   //Название презентации из ресурсов для отладочного режима
const val PageCount = 26       //Количество страниц в презентации, используемой для отладочного режима

const val debugSpeechAudio = R.raw.assembler // Путь к файлу в raw,
                                                  // который запускается в виде тестовой звуковой дорожки.

const val SHARED_PREFERENCES_FILE_NAME = "com.example.company.myapplication.prefs"

class StartPageActivity : AppCompatActivity() {

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_page)

        if(!checkPermissions())
            checkPermissions()

        val sharedPref = getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            if (sharedPref.contains(getString(R.string.audio_recording))) {
                return@with
            }
            putBoolean(getString(R.string.audio_recording), true)
            apply()
        }

        pres1.setOnClickListener{
            val intent = Intent(this, PresentationActivity::class.java)
            startActivity(intent)
        }

        pres2.setOnClickListener{
            val intent = Intent(this, PresentationActivity::class.java)
            startActivity(intent)
        }

        addBtn.setOnClickListener{
            val intent = Intent(this, CreatePresentationActivity::class.java)
            startActivity(intent)
        }

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
}
