package com.example.company.myapplication

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       // setContentView(R.layout.activity_main)

        val i = Intent(this, GetTextForPresentation::class.java)
        startActivity(i)
    }
}
