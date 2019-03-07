package com.example.company.myapplication

import android.content.Intent
import android.content.Intent.EXTRA_STREAM
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.*
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.intent.matcher.IntentMatchers.*
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.runner.AndroidJUnit4
import org.hamcrest.CoreMatchers.anything
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrainingStatisticsTransitionTest {

    @Rule
    @JvmField
    var mIntentsTestRule = IntentsTestRule(StartPageActivity::class.java)
    private val itemIndex = 0

    lateinit var debSl : SharedPreferences.Editor
    lateinit var onMode : String
    lateinit var onAudio: String

    @Before
    fun preparePresentationAndTraining(){
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getTargetContext())
        sharedPreferences.edit()
        onMode = mIntentsTestRule.activity.getString(R.string.deb_pres)
        onAudio = mIntentsTestRule.activity.getString(R.string.deb_speech_audio_key)

        debSl.putBoolean(onMode, true)
        debSl.putBoolean(onAudio, true)

        debSl.apply()
        onView(withId(R.id.addBtn)).perform(click())
        onView(withId(R.id.addPresentation)).perform(click())
        Thread.sleep(2000)
        onData(anything()).inAdapterView(withId(R.id.recyclerview_startpage)).atPosition(itemIndex).perform(click())
        Thread.sleep(2000)
        onView(withId(R.id.finish)).perform(click())
        Thread.sleep(2000)
        onView(withId(R.id.button1)).perform(click())
    }

    @Test
    fun transitionFromListClickTest(){
        onView(withId(R.id.share1)).perform(click())
        //onData(anything()).inAdapterView(withId(R.id.recyclerview_training_history)).atPosition(itemIndex).perform(click())
        intended(hasAction(Intent.ACTION_SEND))
    }

    @After
    fun closeTests(){
        debSl.putBoolean(onMode, false)
        debSl.putBoolean(onAudio, false)
        debSl.apply()
    }
}
