package com.example.company.myapplication

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_start_page.*

class StartPageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_page)

        pres1.setOnClickListener{
            val intent = Intent(this, PresentationActivity::class.java)
            startActivity(intent)
        }

        pres2.setOnClickListener{
            val intent = Intent(this, PresentationActivity::class.java)
            startActivity(intent)
        }

        addBtn.setOnClickListener{
            val intent = Intent(this, CreatePresentationActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item?.itemId
        when (id) {
            R.id.action_settings -> {
                val intent = Intent(this, PreferenceActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
