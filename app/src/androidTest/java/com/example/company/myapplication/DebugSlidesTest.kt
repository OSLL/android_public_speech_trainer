package com.example.company.myapplication

import android.content.ComponentName
import android.content.Context
import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.InstrumentationRegistry.getInstrumentation
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.PreferenceMatchers.withTitle
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.runner.AndroidJUnit4
import com.example.company.myapplication.R.string.*
import android.support.test.uiautomator.UiDevice
import com.example.company.myapplication.R.id.trainingTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugSlidesTest {
    @Rule
    @JvmField
    var mIntentsTestRule = IntentsTestRule<StartPageActivity>(StartPageActivity::class.java)

    @Test
    fun Test(){
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getTargetContext())
        val debSl = sharedPreferences.edit()
        debSl.putBoolean("deb_pres", true)
        debSl.apply()
        onView(withId(R.id.addBtn)).perform(ViewActions.click())
        onView(withText("making_presentation.pdf".substring(0, "making_presentation.pdf".indexOf(".pdf")))).check(matches(isDisplayed()))
        onView(withId(R.id.addPresentation)).perform(ViewActions.click())
        onView(withText("making_presentation.pdf".substring(0, "making_presentation.pdf".indexOf(".pdf")))).check(matches(isDisplayed()))
        onView(withText("26"+":00")).check(matches(isDisplayed()))
        debSl.putBoolean("deb_pres", false)
        debSl.apply()
    }
}