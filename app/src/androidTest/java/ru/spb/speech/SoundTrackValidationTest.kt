package ru.spb.speech

import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiSelector
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Thread.sleep

@RunWith(AndroidJUnit4::class)
class SoundTrackValidationTest {
    private var mDevice: UiDevice? = null
    private var presName = ""

    @Before
    fun before() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertThat(mDevice, CoreMatchers.notNullValue())

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getTargetContext())
        val debSl = sharedPreferences.edit()
        val onMode = mIntentsTestRule.activity.getString(R.string.deb_pres)
        val onAudio = mIntentsTestRule.activity.getString(R.string.deb_speech_audio_key)

        debSl.putBoolean(onMode, true)
        debSl.putBoolean(onAudio, true)

        debSl.apply()
    }

    @Rule
    @JvmField
    var mIntentsTestRule = ActivityTestRule<StartPageActivity>(StartPageActivity::class.java)

    @Test
    fun soundTrackValidationTest(){

        onView(withId(R.id.addBtn)).perform(click())
        presName = mIntentsTestRule.activity.getString(R.string.deb_pres_name).substring (0, mIntentsTestRule.activity.getString(R.string.deb_pres_name).indexOf(mIntentsTestRule.activity.getString(R.string.pdf_format)))
        onView(withId(R.id.presentationName)).perform(clearText(), typeText(presName), closeSoftKeyboard())
        onView(withId(R.id.addPresentation)).perform(click())

        mDevice!!.findObject(UiSelector().text(presName)).click()

        sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.required_time_in_milliseconds_allotted_for_training).toLong())

        mDevice!!.findObject(UiSelector().text(mIntentsTestRule.activity.getString(R.string.stop))).click()

        sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.time_in_milliseconds_until_you_can_switch_to_workout_statistics).toLong())

        mDevice!!.findObject(UiSelector().text(mIntentsTestRule.activity.getString(R.string.training_statistics))).click()

        assertEquals(speed_statistics!!.toFloat(),mIntentsTestRule.activity.resources.getDimension(R.dimen.expected_number_of_recognized_words),mIntentsTestRule.activity.resources.getDimension(R.dimen.error_in_the_number_of_recognized_words))

        sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.time_in_milliseconds_wait_after_test).toLong())

        mDevice!!.pressBack()

        onView(withText(presName)).perform(longClick())
        onView(withText(mIntentsTestRule.activity.getString(R.string.remove))).perform(click())

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getTargetContext())
        val debSl = sharedPreferences.edit()
        val onMode = mIntentsTestRule.activity.getString(R.string.deb_pres)
        val onAudio = mIntentsTestRule.activity.getString(R.string.deb_speech_audio_key)
        debSl.putBoolean(onMode, false)
        debSl.putBoolean(onAudio, false)
        debSl.apply()
    }

}