package ru.spb.speech

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.PickerActions
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.By
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiSelector
import android.support.test.uiautomator.Until
import junit.framework.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.spb.speech.notifications.NotificationsHelper
import java.util.*

@RunWith(AndroidJUnit4::class)
class NotificationsTest {
    val TIMEOUT = 2500L

    @Rule
    @JvmField
    var activityTestRule = ActivityTestRule<StartPageActivity>(StartPageActivity::class.java)

    lateinit var notificationsHelper: NotificationsHelper
    lateinit var testHelper: TestHelper
    lateinit var device: UiDevice

    init {
        grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        grantPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        grantPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    @Before
    fun before() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        testHelper = TestHelper(activityTestRule.activity)
        notificationsHelper = NotificationsHelper(activityTestRule.activity)

        testHelper.setTrainingPresentationMod(true)
    }

    @After
    fun after() {
        testHelper.setTrainingPresentationMod(false)
    }

    @Test
    fun defaultNotificationsCall() {
        val presentationName = testHelper.addDebugPresentation(notifications = true)

        Thread.sleep(TIMEOUT)
        assertTrue(notificationsHelper.validateNotification())

        val calendar = Calendar.getInstance()

        onView(withText(presentationName)).perform(longClick())
        onView(withText(activityTestRule.activity.getString(R.string.change))).perform(click())
        onView(withId(R.id.datePicker)).perform(PickerActions.setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH) - 1))
        onView(withId(R.id.addPresentation)).perform(click())

        Thread.sleep(TIMEOUT)
        assertFalse(notificationsHelper.validateNotification())

        testHelper.removeDebugSlides()
    }

    @Test
    fun notificationExists() {
        val expectedApplicationName = activityTestRule.activity.getString(R.string.app_name)
        val expectedTitle = activityTestRule.activity.getString(R.string.notifications_title)
        val expectedText = activityTestRule.activity.getString(R.string.notifications_text)

        notificationsHelper.sendNotification()

        device.openNotification()
        device.wait(Until.findObject(By.textStartsWith(expectedApplicationName)), TIMEOUT)

        val title = device.findObject(By.text(expectedTitle))
        val text = device.findObject(By.text(expectedText))

        assertEquals(title.text, expectedTitle)
        assertEquals(text.text, expectedText)

        device.findObject(UiSelector().text(expectedApplicationName)).swipeRight(10)
    }

    @Test
    fun tapOnNotification() {
        val expectedApplicationName = activityTestRule.activity.getString(R.string.app_name)

        notificationsHelper.sendNotification()

        device.pressHome()

        device.openNotification()
        device.wait(Until.findObject(By.textStartsWith(expectedApplicationName)), TIMEOUT).click()

        onView(withText(activityTestRule.activity.getString(R.string.activity_start_page_name))).check(matches(isDisplayed()))
    }
}