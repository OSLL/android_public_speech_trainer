package ru.spb.speech.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import ru.spb.speech.R

class LicensesFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): LinearLayout? {
        val v = inflater.inflate(R.layout.licenses_fragment, container, false) as LinearLayout

        for (i in 0 until v.childCount) {
            val tv: TextView
            try { tv = v.getChildAt(i) as TextView } catch (e: TypeCastException) { e.printStackTrace();continue }
            tv.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tv.text.toString()))) }
        }

        return v
    }
}