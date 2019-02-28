package com.example.company.myapplication

import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.IdlingPolicies
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import android.support.test.espresso.Espresso
import android.support.test.espresso.action.ViewActions.click


@RunWith(AndroidJUnit4::class)
class TrainingTimeTest {
    val trainingTime = 50000L // millisecsec
    val precision = 5000L

    @Rule
    @JvmField
    var mIntentsTestRule = ActivityTestRule<StartPageActivity>(StartPageActivity::class.java)

    @Test
    fun test(){
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getTargetContext())
        val debSl = sharedPreferences.edit()
        val OnMode = mIntentsTestRule.activity.getString(R.string.deb_pres)
        val OnAudio = mIntentsTestRule.activity.getString(R.string.deb_speech_audio_key)

        debSl.putBoolean(OnMode, true)
        debSl.putBoolean(OnAudio, true)

        debSl.apply()
        onView(withId(R.id.addBtn)).perform(click())
        onView(withId(R.id.addPresentation)).perform(click())
        onView(withId(android.R.id.button1)).perform(click())

        IdlingPolicies.setMasterPolicyTimeout(trainingTime, TimeUnit.MILLISECONDS)
        IdlingPolicies.setIdlingResourceTimeout(trainingTime, TimeUnit.MILLISECONDS)

        val idlingResource = ElapsedTimeIdlingResource(trainingTime)
        Espresso.registerIdlingResources(idlingResource)

        // check goes here
        debSl.putBoolean(OnMode, false)
        debSl.putBoolean(OnAudio, false)
        debSl.apply()
    }

}