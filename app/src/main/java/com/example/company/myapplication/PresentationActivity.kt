package com.example.company.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_presentation.*

const val TIME_ALLOTTED_FOR_TRAINING = "TrainingTime"
const val MESSAGE_ABOUT_FORMAT_INCORRECTNESS = "Неправильный формат"
const val NAME_OF_PRES = "presentation_name"

class PresentationActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_presentation)

        val DefTime = intent.getIntExtra(DEFAULT_TIME, 0)
        var min = DefTime
        if(min > 999){
            min = 999
        }
        trainingTime.setText(min.toString()+":00")

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
            return a.length < 4 && b.length < 3 && a.toInt() < 1000 && b.toInt() < 60
        }

        val name = intent.getStringExtra(NAME_OF_PRES)
        presentationName.text = name

        val uri = intent.getParcelableExtra<Uri>(URI)

        training.setOnClickListener {
            val i = Intent(this, TrainingActivity::class.java)
            i.putExtra(URI, uri)
            if (SearchSymbol(trainingTime.text.toString())) {
                val min = trainingTime.text.toString().substring(0, trainingTime.text.indexOf(":"))
                val sec = trainingTime.text.toString().substring(trainingTime.text.indexOf(":") + 1,
                        trainingTime.text.lastIndex + 1)
                if (IsNumber(min, sec) && TestLong(min,sec)) {
                    val time = min.toLong() * 60 + sec.toLong()
                    i.putExtra(TIME_ALLOTTED_FOR_TRAINING, time)
                    startActivity(i)
                } else {
                    Toast.makeText(this, MESSAGE_ABOUT_FORMAT_INCORRECTNESS, Toast.LENGTH_SHORT).show()
                    Log.d("error", MESSAGE_ABOUT_FORMAT_INCORRECTNESS)
                }
            } else {
                Toast.makeText(this, MESSAGE_ABOUT_FORMAT_INCORRECTNESS, Toast.LENGTH_SHORT).show()
                Log.d("error", MESSAGE_ABOUT_FORMAT_INCORRECTNESS)
            }
        }

        trainingHistory.setOnClickListener {
            startActivity(Intent(this,TrainingHistoryActivity::class.java))
        }
        //share example
        share.setOnClickListener {
            val sharingIntent = Intent(android.content.Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            val shareBody = "Your body here"
            val shareSub = "Your subject here"
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, shareSub)
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody)
            startActivity(Intent.createChooser(sharingIntent, "Share using"))
        }
        //-------------
    }
}
