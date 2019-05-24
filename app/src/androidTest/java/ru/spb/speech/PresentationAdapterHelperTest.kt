package ru.spb.speech

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.*
import android.support.test.espresso.Espresso.*
import org.junit.*
import org.junit.runner.RunWith
import java.lang.Thread.sleep

@RunWith(AndroidJUnit4::class)
class PresentationAdapterHelperTest : BaseInstrumentedTest() {
    @Rule
    @JvmField
    var activityTestRule = ActivityTestRule<StartPageActivity>(StartPageActivity::class.java)

    lateinit var helper: TestHelper
    lateinit var uiDevice: UiDevice

    @Before
    fun enableDebugMode() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        helper = TestHelper(activityTestRule.activity)
        helper.setTrainingPresentationMod(true) // включение тестовой презентации
    }

    @After
    fun disableDebugMode() {
        helper.setTrainingPresentationMod(false) // выключение тестовой презентации
        helper.removeDebugSlides()
    }

    @Test
    fun startTrainingDialogTest() {
        // Проверка positiveButton
        onView(withId(R.id.addBtn)).perform(click())
        onView(withId(R.id.presentationName)).perform(replaceText(activityTestRule.activity.getString(R.string.making_presentation)))
        onView(withId(R.id.addPresentation)).perform(click())
        sleep(2000)
        helper.startTrainingDialog(uiDevice)
        sleep(2000)
        uiDevice.findObject(UiSelector().text(activityTestRule.activity.getString(R.string.stop))).click()
        sleep(2000)
        onView(withId(android.R.id.button1)).perform(click())
        uiDevice.pressBack()

        // Проверка negativeButton
        onView(withText(R.string.making_presentation)).perform(click())
        sleep(2000)
        onView(withText(R.string.no)).perform(click())
    }
}