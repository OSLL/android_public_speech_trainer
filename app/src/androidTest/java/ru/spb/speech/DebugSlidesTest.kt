package ru.spb.speech

import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.runner.AndroidJUnit4
import ru.spb.speech.database.SpeechDataBase
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugSlidesTest : BaseInstrumentedTest() {
    @Rule
    @JvmField
    var mIntentsTestRule = IntentsTestRule<StartPageActivity>(StartPageActivity::class.java)

    lateinit var helper: TestHelper

    @Before
    fun enableDebugMode() {
        helper = TestHelper(mIntentsTestRule.activity)
        helper.setTrainingPresentationMod(true) // включение тестовой презентации
    }

    @After
    fun disableDebugMode() {
        helper.setTrainingPresentationMod(false) // выключение тестовой презентации
        helper.removeDebugSlides()
    }

    @Test
    fun test(){
        val db = SpeechDataBase.getInstance(getTargetContext())?.PresentationDataDao()
        db?.deleteAll() // удаление всех элементов БД
        assertEquals(db?.getAll()?.size?.toFloat(), 0f) // проверка БД на пустоту
        onView(withId(R.id.addBtn)).perform(ViewActions.click())
        onView(withText(R.string.making_presentation)).check(matches(isDisplayed()))
        onView(withText("26")).check(matches(isDisplayed()))
        onView(withId(R.id.addPresentation)).perform(ViewActions.click())
    }


}