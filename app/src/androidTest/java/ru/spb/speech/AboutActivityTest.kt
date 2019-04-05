package ru.spb.speech

import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.support.test.InstrumentationRegistry.getInstrumentation
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.*
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.scrollTo
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.Intents
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.intent.Intents.intending
import android.support.test.espresso.intent.matcher.IntentMatchers.hasAction
import android.support.test.espresso.intent.matcher.IntentMatchers.hasData
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import ru.spb.speech.R.string.*

class AboutActivityTest {
    @Rule
    @JvmField
    var mControllerTestRule = ActivityTestRule<StartPageActivity>(StartPageActivity::class.java)

    init {
        grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        grantPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        grantPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    @Test
    fun checkButtons() {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)
        getInstrumentation().waitForIdleSync()

        onView(withText(about)).check(matches(isDisplayed()))
        onView(withText(about)).perform(scrollTo(), click())

        onView(withId(R.id.licenses_about)).perform(scrollTo())
        onView(withId(R.id.licenses_about)).check(matches(isDisplayed()))
        onView(withId(R.id.licenses_about)).check(matches(isFocusable()))

        onView(withId(R.id.repository_link_about)).perform(scrollTo())
        onView(withId(R.id.repository_link_about)).check(matches(isDisplayed()))
        onView(withId(R.id.repository_link_about)).check(matches(isFocusable()))
    }

    @Test
    fun checkViews() {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)
        getInstrumentation().waitForIdleSync()

        onView(withText(about)).perform(scrollTo(), click())
        onView(withId(R.id.imageView)).check(matches(isDisplayed()))

        onView(withId(R.id.textView2)).check(matches(isDisplayed()))
        onView(withId(R.id.textView2)).check(matches(withText(app_name)))

        onView(withText(version)).check(matches(isDisplayed()))

        onView(withId(R.id.version_about)).check(matches(isDisplayed()))
        onView(withId(R.id.version_about)).check(matches(withText(
                mControllerTestRule.activity
                        ?.packageManager
                        ?.getPackageInfo(mControllerTestRule.activity.packageName, 0)
                        ?.versionName
        )))

        onView(withId(R.id.textView3)).check(matches(isDisplayed()))
        onView(withId(R.id.textView3)).perform(scrollTo())
        onView(withId(R.id.textView3)).check(matches(withText(about_text)))

        onView(withId(R.id.textView4)).perform(scrollTo())
        onView(withId(R.id.textView4)).check(matches(isDisplayed()))
        onView(withId(R.id.textView4)).check(matches(withText(developers)))

        onView(withId(R.id.textView5)).perform(scrollTo())
        onView(withId(R.id.textView5)).check(matches(isDisplayed()))
        onView(withId(R.id.textView5)).check(matches(withText(authors)))

        onView(withId(R.id.licenses_about)).perform(scrollTo())
        onView(withId(R.id.licenses_about)).check(matches(isDisplayed()))
        onView(withId(R.id.licenses_about)).check(matches(withText(licenses)))

        onView(withId(R.id.repository_link_about)).perform(scrollTo())
        onView(withId(R.id.repository_link_about)).check(matches(isDisplayed()))
        onView(withId(R.id.repository_link_about)).check(matches(withText(repositoryLink)))
    }

    @Test
    fun checkRepoLink() {
        Intents.init()
        val expectedIntent = allOf(hasAction(Intent.ACTION_VIEW),
                hasData(Uri.parse(getTargetContext().getString(R.string.repositoryUrl))))
        intending(expectedIntent).respondWith(Instrumentation.ActivityResult(0, null))

        openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)
        getInstrumentation().waitForIdleSync()
        onView(withText(about)).perform(scrollTo(), click())
        onView(withText(repositoryLink)).perform(scrollTo(), click())

        intended(expectedIntent)
        Intents.release()
    }
}