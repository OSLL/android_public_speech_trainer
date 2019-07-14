package ru.spb.speech

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.assertThat
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiDevice
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class ScreenOrientationCheckTest : BaseInstrumentedTest() {

    private var mDevice: UiDevice? = null

    @Before
    fun before(){
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertThat(mDevice, CoreMatchers.notNullValue())
    }

    @Rule
    @JvmField
    var mIntentsTestRule = IntentsTestRule<StartPageActivity>(StartPageActivity::class.java)



    @Test
    fun checkThatOrientationDoesNotChange() {
        val orientationBefore = mIntentsTestRule.activity.resources.configuration.orientation
        mDevice?.setOrientationLeft()
        val orientationAfter = mIntentsTestRule.activity.resources.configuration.orientation
        Assert.assertEquals(orientationBefore, orientationAfter)
    }
}