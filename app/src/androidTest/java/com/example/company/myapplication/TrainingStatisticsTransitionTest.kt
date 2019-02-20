package com.example.company.myapplication

import android.content.ComponentName
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.*
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.runner.AndroidJUnit4
import org.hamcrest.CoreMatchers.anything
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrainingStatisticsTransitionTest {

    @Rule
    @JvmField
    var mIntentsTestRule = IntentsTestRule(TrainingHistoryActivity::class.java)
    private val itemIndex = 0

    @Test
    fun transitionFromListClickTest(){
        // onView(withId(R.id.recyclerview_training_history)).perform(click())
        onData(anything()).inAdapterView(withId(R.id.recyclerview_training_history)).atPosition(itemIndex).perform(click())
        intended(hasComponent(ComponentName(getTargetContext(), TrainingStatisticsTransitionTest::class.java)))
    }
}