package com.example.company.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_presentation.*

class PresentationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_presentation)

        val name = intent.getStringExtra("presentation_name")
        presentationName.text = name

        val uri = intent.getParcelableExtra<Uri>("presentation_uri")

        training.setOnClickListener {
            val i = Intent(this, TrainingActivity::class.java)
            i.putExtra("presentation_uri", uri)
            startActivity(i)
        }

        trainingHistory.setOnClickListener {
            startActivity(Intent(this,TrainingHistoryActivity::class.java))
        }
    }
}
