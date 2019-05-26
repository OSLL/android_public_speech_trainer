package ru.spb.speech

import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiSelector
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Thread.sleep
import android.support.test.espresso.matcher.ViewMatchers.*
import org.junit.After
import ru.spb.speech.database.SpeechDataBase


@RunWith(AndroidJUnit4::class)
class DisplayOfAssessmentInformationTest : BaseInstrumentedTest(){

    private var mDevice: UiDevice? = null
    private var presName = ""
    private val tContext = InstrumentationRegistry.getTargetContext()
    private val db: SpeechDataBase = SpeechDataBase.getInstance(tContext)!!

    init {
        db.PresentationDataDao().deleteTestPresentations()
    }

    @Before
    fun before() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertThat(mDevice, CoreMatchers.notNullValue())

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getTargetContext())
        val debSl = sharedPreferences.edit()
        val OnMode = mIntentsTestRule.activity.getString(R.string.deb_pres)

        debSl.putBoolean(OnMode, true)
        debSl.putBoolean(mIntentsTestRule.activity.getString(R.string.useStatistics), true)

        debSl.apply()
    }

    @After
    fun after(){
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getTargetContext())
        val debSl = sharedPreferences.edit()
        val OnMode = mIntentsTestRule.activity.getString(R.string.deb_pres)

        debSl.putBoolean(OnMode, false)
        debSl.apply()

        mDevice!!.pressBack()
        mDevice!!.pressBack()

        onView(withText(presName)).perform(longClick())
        sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.time_in_milliseconds_to_display_the_delete_button).toLong())
        onView(withText(mIntentsTestRule.activity.getString(R.string.remove))).perform(click())
    }

    @Rule
    @JvmField
    var mIntentsTestRule = IntentsTestRule<StartPageActivity>(StartPageActivity::class.java)

    @Test
    fun displayInformation() {

        onView(withId(R.id.addBtn)).perform(click())
        presName = mIntentsTestRule.activity.getString(R.string.deb_pres_name).substring (mIntentsTestRule.activity.resources.getInteger(R.integer.zero), mIntentsTestRule.activity.getString(R.string.deb_pres_name).indexOf(mIntentsTestRule.activity.getString(R.string.pdf_format)))
        onView(withId(R.id.presentationName)).perform(clearText(), typeText(presName), closeSoftKeyboard())
        onView(withId(R.id.addPresentation)).perform(click())

        mDevice!!.findObject(UiSelector().text(presName)).click()
        sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.time_in_milliseconds_to_load_the_workout_page).toLong())
        mDevice!!.findObject(UiSelector().text(mIntentsTestRule.activity.getString(R.string.stop))).click()
        sleep(mIntentsTestRule.activity.resources.getInteger(R.integer.time_in_milliseconds_until_you_can_switch_to_workout_statistics).toLong())
        onView(withId(android.R.id.button1)).perform(click())

        //onView(withId(R.id.question)).perform(click())
        onView(withText(R.string.assessment_for_training_title)).check(matches(isDisplayed()))

    }
}
