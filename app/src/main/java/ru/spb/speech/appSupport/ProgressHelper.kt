package ru.spb.speech.appSupport

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import ru.spb.speech.R

class ProgressHelper {

    private var root: FrameLayout
    private var progressView: View
    private var subviewList: List<View>

    constructor(ctx: Context, rootView: FrameLayout, subviews: List<View>) {
        progressView = View.inflate(ctx, R.layout.progress_view, null)
        subviewList = subviews
        root = rootView
    }

    fun show() {
        for (v in subviewList) {
            v.visibility = View.INVISIBLE
        }
        root.addView(progressView)
    }

    fun hide() {
        for (v in subviewList) {
            v.visibility = View.VISIBLE
        }

        root.removeView(progressView)
    }
}