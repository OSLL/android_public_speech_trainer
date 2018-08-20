package com.example.company.myapplication

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_add_presentation.*

class AddPresentationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_presentation)

        TVpresentationText.text = intent.getStringExtra("presentation_txt")
        editPresentationName.setOnFocusChangeListener { v, hasFocus ->
            if (editPresentationName.text.toString() == "Enter the Name")
                editPresentationName.setText("")

            editPresentationName.setTextColor(Color.BLACK)
            editPresentationName.setTypeface(Typeface.DEFAULT)
        }

        add_presentation_btn.setOnClickListener{
            val i = Intent(this, PresentationActivity::class.java)
            i.putExtra("presentation_txt", TVpresentationText.text.toString())
            i.putExtra("presentation_name",editPresentationName.text.toString())
            startActivity(i)
        }
    }
}
