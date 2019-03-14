package ru.spb.speech

import android.support.test.rule.ActivityTestRule
import android.app.Activity


class ControlledActivityTestRule<T : Activity> : ActivityTestRule<T> {
    constructor(activityClass: Class<T>) : super(activityClass, false) {}

    constructor(activityClass: Class<T>, initialTouchMode: Boolean) : super(activityClass, initialTouchMode, true) {}

    constructor(activityClass: Class<T>, initialTouchMode: Boolean, launchActivity: Boolean) : super(activityClass, initialTouchMode, launchActivity) {}

    fun finish() {
        finishActivity()
    }

    fun relaunchActivity() {
        finishActivity()
        launchActivity()
    }

    fun launchActivity() {
        launchActivity(activityIntent)
    }
}