package com.example.company.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.company.myapplication.views.PresentationStartpageRow
import com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces.PresentationDataDao
import com.example.putkovdimi.trainspeech.DBTables.PresentationData
import com.example.putkovdimi.trainspeech.DBTables.SpeechDataBase
import kotlinx.android.synthetic.main.activity_presentation.*

const val TIME_ALLOTTED_FOR_TRAINING = "TrainingTime"
const val MESSAGE_ABOUT_FORMAT_INCORRECTNESS = "Неправильный формат"
const val NAME_OF_PRES = "presentation_name"

class PresentationActivity : AppCompatActivity() {
    private var presentationDataDao: PresentationDataDao? = null
    private var presentationData: PresentationData? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_presentation)

        presentationDataDao = SpeechDataBase.getInstance(this)?.PresentationDataDao()
        val presId = intent.getIntExtra(getString(R.string.CURRENT_PRESENTATION_ID),-1)
        if (presId > 0)
            presentationData = presentationDataDao?.getPresentationWithId(presId)
        else {
            Log.d(TEST_DB, "presentation_act: wrong ID")
            return
        }

        val changePresentationFlag = intent.getIntExtra(getString(R.string.changePresentationFlag), -1) == PresentationStartpageRow.activatedChangePresentationFlag

        if (changePresentationFlag) {
            training.text = getString(R.string.save)
            share.visibility = View.INVISIBLE
            trainingHistory.visibility = View.INVISIBLE
            training.visibility = View.GONE
        }

        if (presentationData?.timeLimit == null) {
            val DefTime = presentationData?.pageCount!!
            var min = DefTime
            if (min > 999) {
                min = 999
            }
            trainingTime.setText(min.toString() + ":00")
        }
        else {
            trainingTime.setText((presentationData?.timeLimit!!.toInt()/60).toString() + ":00")
        }

        fun IsNumber(a: String, b: String): Boolean {
            try {
                a.toInt()
                b.toInt()
                return true
            } catch (e: NumberFormatException){
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

        presentationNameActivityPresentation.text = presentationData?.name
        val uri = Uri.parse(presentationData?.stringUri)

        training.setOnClickListener {
            if (SearchSymbol(trainingTime.text.toString())) {
                val min = trainingTime.text.toString().substring(0, trainingTime.text.indexOf(":"))
                val sec = trainingTime.text.toString().substring(trainingTime.text.indexOf(":") + 1,
                        trainingTime.text.lastIndex + 1)
                if (IsNumber(min, sec) && TestLong(min, sec)) {
                    val time = min.toLong() * 60 + sec.toLong()
                    presentationData?.timeLimit = time
                    presentationDataDao?.updatePresentation(presentationData!!)
                    
                    val i = Intent(this, TrainingActivity::class.java)
                    i.putExtra(getString(R.string.CURRENT_PRESENTATION_ID), presentationData?.id)
                    //i.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
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

        main_btn_activity_presentation.setOnClickListener {
            if (SearchSymbol(trainingTime.text.toString())) {
                val min = trainingTime.text.toString().substring(0, trainingTime.text.indexOf(":"))
                val sec = trainingTime.text.toString().substring(trainingTime.text.indexOf(":") + 1,
                        trainingTime.text.lastIndex + 1)
                if (IsNumber(min, sec) && TestLong(min, sec)) {
                    val time = min.toLong() * 60 + sec.toLong()
                    presentationData?.timeLimit = time
                    presentationDataDao?.updatePresentation(presentationData!!)

                    val i = Intent(this, StartPageActivity::class.java)
                    i.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
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
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.putExtra(Intent.EXTRA_STREAM,  Uri.parse(url))
            sharingIntent.type = "image/jpg"
            startActivity(Intent.createChooser(sharingIntent, "Share with friends"))
        }
        //-------------
    }
}
