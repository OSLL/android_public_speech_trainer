package com.example.putkovdimi.addtoolbarspeectrainer

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.setting_toolbar.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setContentView(R.layout.setting_toolbar)

        setSupportActionBar(settingToolbar)
        Log.d("Toolbar", "onSupportFunc is initialized")
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        Log.d("Toolbar", "is fully ready for action bar's tasks")

    }

    override fun onSupportNavigateUp(): Boolean {
        Log.d("Toolbar", "onSupportFunc is called")
        onBackPressed()
        return true
    }
}

