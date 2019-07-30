package ru.spb.speech

import android.content.ComponentName
import android.support.test.InstrumentationRegistry
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.intent.matcher.IntentMatchers
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiSelector
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class RecommendationActivityTest : BaseInstrumentedTest() {

    lateinit var helper: TestHelper
    lateinit var uiDevice: UiDevice

    @Rule
    @JvmField
    var mIntentsTestRule = IntentsTestRule<StartPageActivity>(StartPageActivity::class.java)

    @Before
    fun before() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        helper = TestHelper(mIntentsTestRule.activity)
        helper.setTrainingPresentationMod(true) // включение тестовой презентации

        onView(withId(R.id.addBtn)).perform(ViewActions.click())
        Thread.sleep(2000)
        onView(withId(R.id.presentationName)).perform(ViewActions.replaceText(mIntentsTestRule.activity.getString(R.string.making_presentation)))
        Thread.sleep(2000)
        onView(withId(R.id.addPresentation)).perform(ViewActions.click())

        Thread.sleep(2000)
        helper.startTrainingDialog(uiDevice)
        Thread.sleep(2000)
        uiDevice.findObject(UiSelector().text(mIntentsTestRule.activity.getString(R.string.stop))).click()
        Thread.sleep(2000)
        onView(withId(android.R.id.button1)).perform(ViewActions.click())
        Thread.sleep(2000)
        onView(withId(R.id.improve_mark_button)).perform(ViewActions.click())
        Thread.sleep(2000)
    }

    @After
    fun after(){
        helper.setTrainingPresentationMod(false) // выключение тестовой презентации
        helper.removeDebugSlides()
    }


    @Test
    fun intentIsDisplayedCheck(){
        onView(withId(R.id.backToStatistics)).perform(ViewActions.scrollTo()).perform(ViewActions.click())
        Thread.sleep(2000)
        intended(IntentMatchers.hasComponent(ComponentName(getTargetContext(), TrainingStatisticsActivity::class.java)))

        onView(withId(R.id.improve_mark_button)).perform(ViewActions.click())
        Thread.sleep(2000)

        onView(withId(R.id.toHomeScreen)).perform(ViewActions.scrollTo()).perform(ViewActions.click())
        Thread.sleep(2000)
        intended(IntentMatchers.hasComponent(ComponentName(getTargetContext(), StartPageActivity::class.java)))
    }

    @Test
    fun textViewAndButtonWithTextCheck() {
        onView(withId(R.id.recommendationLabel)).check(matches(withText(mIntentsTestRule.activity.getString(R.string.performance_recommendations))))
        onView(withId(R.id.slidesTimeLabel)).check(matches(withText(mIntentsTestRule.activity.getString(R.string.slide_time))))
//        onView(withId(R.id.slidesTimeRecommendation)).perform(ViewActions.scrollTo()).check(matches(withText("Рекомендации по времени слайдов - заглушка")))
        onView(withId(R.id.slidesFrequency)).perform(ViewActions.scrollTo()).check(matches(withText(mIntentsTestRule.activity.getString(R.string.slides_frequency))))
//        onView(withId(R.id.slidesFrequencyRecommendation)).perform(ViewActions.scrollTo()).check(matches(withText("Рекомендации по частоте слов - заглушка")))
        onView(withId(R.id.scumWordsLabel)).perform(ViewActions.scrollTo()).check(matches(withText(mIntentsTestRule.activity.getString(R.string.scum_words_label))))
//        onView(withId(R.id.scumWordsRecommendation)).perform(ViewActions.scrollTo()).check(matches(withText("Рекомендации по словам-паразитам - заглушка")))
        onView(withId(R.id.backToStatistics)).perform(ViewActions.scrollTo()).check(matches(withText(mIntentsTestRule.activity.getString(R.string.back_to_statistics))))
        onView(withId(R.id.toHomeScreen)).perform(ViewActions.scrollTo()).check(matches(withText(mIntentsTestRule.activity.getString(R.string.to_home_screen))))
        Thread.sleep(2000)
        onView(withId(R.id.toHomeScreen)).perform(ViewActions.scrollTo()).perform(ViewActions.click())
        Thread.sleep(2000)
    }
}