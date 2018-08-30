package com.example.company.myapplication

import android.content.ComponentName
import android.support.test.InstrumentationRegistry
import android.support.test.InstrumentationRegistry.getInstrumentation
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.*
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.runner.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Rule

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @Rule
    @JvmField
    var mIntentsTestRule = IntentsTestRule<StartPageActivity>(StartPageActivity::class.java)

    @Test
    fun buttonsExist() {
        onView(withId(R.id.addBtn)).check(matches(isDisplayed()))
        onView(withText("Презентация 1")).check(matches(isDisplayed()))
        onView(withId(R.id.pres1)).check(matches(isDisplayed()))
        onView(withText("Презентация 2")).check(matches(isDisplayed()))
        onView(withId(R.id.pres2)).check(matches(isDisplayed()))
        onView(withText("+")).check(matches(isDisplayed()))
    }

    @Test
    fun test_from_start_page_to_presentation_with_pres1(){
        onView(withId(R.id.pres1)).perform(ViewActions.click())
        intended(hasComponent(ComponentName(getTargetContext(), PresentationActivity::class.java)))
    }

    @Test
    fun test_from_start_page_to_presentation_with_pres2(){
        onView(withId(R.id.pres2)).perform(ViewActions.click())
        intended(hasComponent(ComponentName(getTargetContext(), PresentationActivity::class.java)))
    }

    @Test
    fun test_from_start_page_to_preference(){
        openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)
        onView(withText("Настройки")).perform(click());
        intended(hasComponent(ComponentName(getTargetContext(), PreferenceActivity::class.java)))
    }
}
