package ru.spb.speech.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import ru.spb.speech.R

class AboutFragment: Fragment(), View.OnTouchListener {

    private var x1 = 0f
    private var y1 = 0f
    private val distanceLimit = 150f

    lateinit var openLicenses: () -> Unit

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.about_fragment, container, false)

        v.findViewById<TextView>(R.id.licenses_about).setOnTouchListener(this)
        v.findViewById<TextView>(R.id.repository_link_about).setOnTouchListener(this)

        v.findViewById<TextView>(R.id.version_about).text = activity
                ?.packageManager
                ?.getPackageInfo(activity?.packageName, 0)
                ?.versionName

        return v
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        val licenses = v?.id == R.id.licenses_about
        val repository = v?.id == R.id.repository_link_about

        return if (licenses || repository) {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> { v?.alpha = 0.33f;x1 = event.x;y1 = event.y }

                MotionEvent.ACTION_UP -> {
                    if (isClick(x1, y1, event.x, event.y)) {
                        if (repository) startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.repositoryUrl))))
                        if (licenses) openLicenses.invoke()
                    }
                    v?.alpha = 1f;x1 = event.x;y1 = event.y
                }

                MotionEvent.ACTION_CANCEL -> { v?.alpha = 1f;x1 = event.x;y1 = event.y }
            }
            true
        } else false
    }

    private fun isClick(x1: Float, y1: Float, x2: Float, y2: Float): Boolean
            = Math.sqrt(Math.pow((x1 - x2).toDouble(), 2.0) + Math.pow((y1 - y2).toDouble(), 2.0)) < distanceLimit
}