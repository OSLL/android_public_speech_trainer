package com.example.company.myapplication

import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.content.Intent.CATEGORY_OPENABLE
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import java.io.FileNotFoundException


class CreatePresentationActivity : AppCompatActivity() {

    private val REQUSETCODE = 111

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
        val myUri = Uri.parse(path)
        val intent = Intent(ACTION_GET_CONTENT)
                .setDataAndType(myUri, "*/*")
                .addCategory(CATEGORY_OPENABLE)

        startActivityForResult(Intent.createChooser(intent, "Select a file"), REQUSETCODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUSETCODE && resultCode == RESULT_OK && data != null) {
            val selectedFile = data.data //The uri with the location of the file
            try {
                val i1 = Intent(this, EditPresentationActivity::class.java)
                i1.putExtra("presentation_uri1", selectedFile)
                startActivity(i1)
            } catch (e: FileNotFoundException) {
                Log.d("file_system", "file not found")
            }
        }
    }
}

