package com.example.company.myapplication

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_edit_presentation.*

class EditPresentationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_presentation)

        presentationText.text = intent.getStringExtra("presentation_txt")

        addPresentation.setOnClickListener{
            if (presentationName.text.toString() == ""){
                Toast.makeText(this, "Please Enter Presentation Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val i = Intent(this, PresentatinActivity::class.java)
            i.putExtra("presentation_txt", presentationText.text.toString())
            i.putExtra("presentation_name",presentationName.text.toString())
            startActivity(i)
        }
    }
}
