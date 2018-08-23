package com.example.company.myapplication

import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.example.company.myapplication.R.id.addPresentation
import com.example.company.myapplication.R.id.presentationName
import kotlinx.android.synthetic.main.activity_edit_presentation.*

class EditPresentationActivity : AppCompatActivity() {

    private var renderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_presentation)


        val uri_path= intent.getStringExtra("presentation_uri")

       addPresentation.setOnClickListener{
             if (presentationName.text.toString() == ""){
                Toast.makeText(this, "Please Enter Presentation Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val i = Intent(this, PresentationActivity::class.java)
            i.putExtra("presentation_name",presentationName.text.toString())
            startActivity(i)
        }
    }
}
