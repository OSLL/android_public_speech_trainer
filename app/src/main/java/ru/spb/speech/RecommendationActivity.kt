package ru.spb.speech

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_recommendation.*

class RecommendationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendation)

        val listOfParasites = intent.getStringArrayListExtra("listOfParasites")

        var stringOfParasites = ""
        for (word in listOfParasites)
            stringOfParasites += word + "\n"
        if (stringOfParasites.isEmpty()) {
            scumWordsLabel.visibility = View.GONE
            scumWordsRecommendation.visibility = View.GONE
        }
        else scumWordsRecommendation.text = getString(R.string.scum_words_rec) + "\n" + stringOfParasites

        backToStatistics.setOnClickListener {
            finish()
        }

        toHomeScreen.setOnClickListener {
            val intent = Intent(this, StartPageActivity::class.java)
            startActivity(intent)
        }
    }
}
