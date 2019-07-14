package ru.spb.speech

import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.runner.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UsageDataCollectionTest : BaseInstrumentedTest() {

    @Rule
    @JvmField
    var mIntentsTestRule = IntentsTestRule<StartPageActivity>(StartPageActivity::class.java)

    lateinit var helper: TestHelper

    @Before
    fun enableDebugMode() {
        helper = TestHelper(mIntentsTestRule.activity)
    }

    @After
    fun disableDebugMode() {
        helper.setTrainingPresentationMod(false) // выключение тестовой презентации
    }

    @Test
    fun testUsageDataCollection(){
        val sp = PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getTargetContext())
        val spe = sp.edit()
        val testPresentationMode = mIntentsTestRule.activity.getString(R.string.deb_pres)
        val testPresentationAudio = mIntentsTestRule.activity.getString(R.string.deb_speech_audio_key)

        spe.putBoolean(testPresentationMode, true)
        spe.putBoolean(testPresentationAudio, true)
        spe.apply()

        if (sp.getBoolean(mIntentsTestRule.activity.getString(R.string.first_run), true)) {
            Espresso.onView(ViewMatchers.withText(mIntentsTestRule.activity.getString(R.string.good)))
                    .perform(ViewActions.click())
            spe.putBoolean(mIntentsTestRule.activity.getString(R.string.first_run), false).apply()
        }
    }
}
