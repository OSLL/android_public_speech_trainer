package ru.spb.speech

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.scrollTo
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.runner.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.support.test.espresso.util.HumanReadables
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.ViewAssertion
import org.junit.After
import org.junit.Before


@RunWith(AndroidJUnit4::class)
class ScrollViewTest : BaseInstrumentedTest() {
    @Rule
    @JvmField
    var mIntentsTestRule = IntentsTestRule<TrainingStatisticsActivity>(TrainingStatisticsActivity::class.java)

    lateinit var helper: TestHelper

    @Before
    fun enableDebugMode() {
        helper = TestHelper(mIntentsTestRule.activity)
        helper.setTrainingPresentationMod(true) // включение тестовой презентации
    }

    @After
    fun disableDebugMode() {
        helper.setTrainingPresentationMod(false) // выключение тестовой презентации
    }

    @Test
    fun test(){
        onView(withText(mIntentsTestRule.activity.getString(R.string.share))).check(isNotDisplayed())
        onView(withId(R.id.share1))
                .perform(scrollTo(), click())
        onView(withText(mIntentsTestRule.activity.getString(R.string.share))).check(matches(isDisplayed()))
    }

    fun isNotDisplayed(): ViewAssertion {
        return ViewAssertion { view, _ ->
            if (view != null && isDisplayed().matches(view)) {
                throw AssertionError("View is present in the hierarchy and Displayed: " + HumanReadables.describe(view))
            }
        }
    }

}