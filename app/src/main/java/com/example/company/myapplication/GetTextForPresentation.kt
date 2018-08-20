package com.example.company.myapplication

import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.content.Intent.CATEGORY_OPENABLE
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader

class GetTextForPresentation : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
        Log.d("file_system", path)
        val myUri = Uri.parse(path)
        val intent = Intent(ACTION_GET_CONTENT)
                .setDataAndType(myUri, "*/*")
                .addCategory(CATEGORY_OPENABLE)

        startActivityForResult(Intent.createChooser(intent, "Select a file"), 111)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 111 && resultCode == RESULT_OK && data != null) {
            val selectedFile = data.data //The uri with the location of the file

            try {
                val presentation_txt = readTextFromUri(selectedFile!!)
                val i = Intent(this, AddPresentationActivity::class.java)
                i.putExtra("presentation_txt", presentation_txt)
                Log.d("file_system", presentation_txt.length.toString())

                //если файл очень большой ~ 500kb происходит краш
                startActivity(i)

            } catch (e: FileNotFoundException) {
                Log.d("file_system", "file not found")
            }
        }
    }

    @Throws(IOException::class)
    private fun readTextFromUri(uri: Uri): String {
            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            return reader.use { it.readText() }
    }
}
