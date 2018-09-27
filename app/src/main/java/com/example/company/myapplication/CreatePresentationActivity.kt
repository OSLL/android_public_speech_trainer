package com.example.company.myapplication

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.content.Intent.CATEGORY_OPENABLE
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import java.io.FileNotFoundException

const val URI = "presentation_uri"
const val FILE_SYSTEM = "file_system"

class CreatePresentationActivity : AppCompatActivity() {

    private val REQUSETCODE = 111

    override fun onCreate(savedInstanceState: Bundle?) {
        val sPref = getPreferences(Context.MODE_PRIVATE)
        super.onCreate(savedInstanceState)
        if(sPref.getString(getString(R.string.DEBUG_SLIDES), debugSlides) == "") {
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
            val myUri = Uri.parse(path)
            val intent = Intent(ACTION_GET_CONTENT)
                    .setDataAndType(myUri, "*/*")
                    .addCategory(CATEGORY_OPENABLE)

            startActivityForResult(Intent.createChooser(intent, "Select a file"), REQUSETCODE)
        } else {
            val i = Intent(this, EditPresentationActivity::class.java)
            i.putExtra(URI, sPref.getString(getString(R.string.DEBUG_SLIDES), debugSlides))
            startActivity(i)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
  //      val sPref = getPreferences(Context.MODE_PRIVATE)
 //       if(!sPref.getBoolean(getString(R.string.DEBUG_SLIDES), debugSlides)) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == REQUSETCODE && resultCode == RESULT_OK && data != null) {
                val selectedFile = data.data //The uri with the location of the file
                try {
                    val i = Intent(this, EditPresentationActivity::class.java)
                    i.putExtra(URI, selectedFile)
                    Log.d(FILE_SYSTEM, selectedFile.toString())
                    startActivity(i)
                } catch (e: FileNotFoundException) {
                    Log.d(FILE_SYSTEM, "file not found")
                }
            }
   //     }
    }
}

