package com.example.company.myapplication

import android.content.ComponentName
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
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.runner.AndroidJUnit4
import com.example.company.myapplication.R.string.*
import android.support.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class StartPageActivityTest {

    @Rule
    @JvmField
    var mIntentsTestRule = IntentsTestRule<StartPageActivity>(StartPageActivity::class.java)

    @Test
    fun buttonsExist() {
        onView(withId(R.id.addBtn)).check(matches(isDisplayed()))
        onView(withText(first_presentation)).check(matches(isDisplayed()))
        onView(withId(R.id.pres1)).check(matches(isDisplayed()))
        onView(withText(second_presentation)).check(matches(isDisplayed()))
        onView(withId(R.id.pres2)).check(matches(isDisplayed()))
        onView(withText(add_presentation_symbol)).check(matches(isDisplayed()))
    }

    @Test
    fun test_from_start_page_to_presentation_with_pres1(){
        onView(withId(R.id.pres1)).perform(click())
        intended(hasComponent(ComponentName(getTargetContext(), PresentationActivity::class.java)))
    }

    @Test
    fun test_from_start_page_to_presentation_with_pres2(){
        onView(withId(R.id.pres2)).perform(click())
        intended(hasComponent(ComponentName(getTargetContext(), PresentationActivity::class.java)))
    }

    @Test
    fun test_from_start_page_to_preference(){
        openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)
        onView(withText(activity_preference)).perform(click())
        intended(hasComponent(ComponentName(getTargetContext(), SettingsActivity::class.java)))
    }

    @Test
    fun test_from_start_page_to_open_file_dialog(){
        onView(withId(R.id.addBtn)).perform(ViewActions.click())
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        intended(hasComponent(ComponentName(getTargetContext(), CreatePresentationActivity::class.java)))
    }
}
