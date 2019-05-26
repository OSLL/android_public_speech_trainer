package ru.spb.speech

import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiSelector
import ru.spb.speech.database.SpeechDataBase
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugSlidesTest : BaseInstrumentedTest() {

    private var mDevice: UiDevice? = null
    private var presName = ""

    @Rule
    @JvmField
    var mIntentsTestRule = IntentsTestRule<StartPageActivity>(StartPageActivity::class.java)

    @Test
    fun Test(){
        val db = SpeechDataBase.getInstance(getTargetContext())?.PresentationDataDao()
        db?.deleteAll() // удаление всех элементов БД
        assertEquals(db?.getAll()?.size?.toFloat(), 0f) // проверка БД на пустоту
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getTargetContext())
        val debSl = sharedPreferences.edit()
        val onMode = mIntentsTestRule.activity.getString(R.string.deb_pres)
        val presName = mIntentsTestRule.activity.getString(R.string.deb_pres_name)
        debSl.putBoolean(onMode, true)
        debSl.apply()
        onView(withId(R.id.addBtn)).perform(ViewActions.click())
        onView(withText(presName.substring(0, presName.indexOf(".pdf")))).check(matches(isDisplayed()))
        onView(withText("26")).check(matches(isDisplayed()))
        onView(withId(R.id.addPresentation)).perform(ViewActions.click())
        debSl.putBoolean(onMode, false)
        debSl.apply()
    }

    @Test
    fun TestExportFlag(){
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertThat(mDevice, CoreMatchers.notNullValue())

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getTargetContext())
        val debSl = sharedPreferences.edit()
        val onMode = mIntentsTestRule.activity.getString(R.string.deb_pres)
        val onAudio = mIntentsTestRule.activity.getString(R.string.deb_speech_audio_key)
        val exportFlagCheck = "deb_statistics_export"

        debSl.putBoolean(onMode, true)
        debSl.putBoolean(onAudio, true)
        debSl.putBoolean(mIntentsTestRule.activity.getString(R.string.useStatistics), true)
        debSl.putBoolean(exportFlagCheck, true)

        debSl.apply()

        onView(withId(R.id.addBtn)).perform(ViewActions.click())

        presName = mIntentsTestRule.activity.getString(R.string.deb_pres_name).substring(mIntentsTestRule.activity.resources.getInteger(R.integer.zero), mIntentsTestRule.activity.getString(R.string.deb_pres_name).indexOf(mIntentsTestRule.activity.getString(R.string.pdf_format)))

        onView(withId(R.id.presentationName)).perform(ViewActions.clearText(), ViewActions.typeText(presName), ViewActions.closeSoftKeyboard())
        onView(withId(R.id.addPresentation)).perform(ViewActions.click())

        mDevice!!.findObject(UiSelector().text(presName)).click()
        Thread.sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.workout_time_in_milliseconds_for_training).toLong())

        mDevice!!.findObject(UiSelector().text(mIntentsTestRule.activity.getString(R.string.stop))).click()
        Thread.sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.time_in_milliseconds_until_you_can_switch_to_workout_statistics).toLong())

        onView(withId(android.R.id.button1)).perform(ViewActions.click())

        onView(withId(R.id.export)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        debSl.putBoolean(onMode, false)
        debSl.putBoolean(onAudio, false)
        debSl.putBoolean(exportFlagCheck, false)
        debSl.apply()

        mDevice!!.pressBack()

        onView(withText(presName)).perform(ViewActions.longClick())
        Thread.sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.time_in_milliseconds_to_display_the_delete_button).toLong())

        onView(withText(mIntentsTestRule.activity.getString(R.string.remove))).perform(ViewActions.click())
    }
}