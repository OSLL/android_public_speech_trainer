package com.example.company.myapplication

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_training.*

class TrainingActivity : AppCompatActivity() {

    var slideIndex = 1
    var slideCount = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)
        next.isEnabled = true
        finish.isEnabled = false

        updateSlide()

        next.setOnClickListener {
            if (slideIndex < slideCount) {
                slideIndex++
                updateSlide()
            }
            if (slideIndex == slideCount){
                next.isEnabled = false
                finish.isEnabled = true
            }
        }

        finish.setOnClickListener {
            startActivity(Intent(this,TrainingStatisticsActivity::class.java))
        }
    }

    private fun updateSlide() {
        time_left.text = "$slideIndex / $slideCount / Осталось времени:"
    }
}