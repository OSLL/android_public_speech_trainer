package ru.spb.speech

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import ru.spb.speech.fragments.AboutFragment
import ru.spb.speech.fragments.LicensesFragment

class AboutActivity : AppCompatActivity() {

    private lateinit var aboutFragment: AboutFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        aboutFragment = AboutFragment()

        aboutFragment.openLicenses = {
            title = getString(R.string.licenses)
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.root_about,LicensesFragment(), LicensesFragment::class.toString())
                    .commit()
        }

        supportFragmentManager.beginTransaction()
                .add(R.id.root_about, aboutFragment, AboutFragment::class.toString())
                .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return if (item?.itemId == android.R.id.home && supportFragmentManager.fragments.size == 1) {
            super.onBackPressed()
            true
        } else {
            removeLicensesFragment()
            false
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.fragments.size > 1) removeLicensesFragment()
        else super.onBackPressed()
    }

    private fun removeLicensesFragment() {
        title = getString(R.string.about)
        supportFragmentManager.beginTransaction()
                .remove(supportFragmentManager.findFragmentByTag(LicensesFragment::class.toString()))
                .commit()
    }
}
