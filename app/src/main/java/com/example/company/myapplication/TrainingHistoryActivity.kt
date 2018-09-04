package com.example.company.myapplication

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_training_history.*

class TrainingHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_history)

        training1.setOnClickListener{
            val intent = Intent(this, TrainingStatisticsActivity::class.java)
            startActivity(intent)
        }

        training2.setOnClickListener{
            val intent = Intent(this, TrainingStatisticsActivity::class.java)
            startActivity(intent)
        }
    }
}
