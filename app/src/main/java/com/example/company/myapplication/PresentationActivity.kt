package com.example.company.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_presentation.*

class PresentationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_presentation)

        fun IsNumber(a: String, b: String): Boolean {
            try {
                a.toInt()
                b.toInt()
                return true
            } catch (e: NumberFormatException) {
                return false
            }
        }

        fun SearchSymbol(s: String): Boolean{
            var marker = 0
            for (i in s){
                if (i.toString() == ":"){
                    marker = 1
                }
            }
            return marker == 1
        }

        fun TestLong(a: String, b: String): Boolean{
            return a.length < 3 && b.length < 3 && a.toInt() < 61 && b.toInt() < 61
        }

        val name = intent.getStringExtra("presentation_name")
        presentationName.text = name

        val uri = intent.getParcelableExtra<Uri>("presentation_uri2")

        training.setOnClickListener {
            val i = Intent(this, TrainingActivity::class.java)
            i.putExtra("presentation_uri3", uri)
            if (SearchSymbol(trainingTime.text.toString())) {
                val min = trainingTime.text.toString().substring(0, trainingTime.text.indexOf(":"))
                val sec = trainingTime.text.toString().substring(trainingTime.text.indexOf(":") + 1,
                        trainingTime.text.lastIndex + 1)
                if (IsNumber(min, sec) && TestLong(min,sec)) {
                    val time = min.toLong() * 60 + sec.toLong()
                    i.putExtra("TrainingTime", time)
                    startActivity(i)
                } else {
                    Toast.makeText(this, "The format is incorrect", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "The format is incorrect", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
