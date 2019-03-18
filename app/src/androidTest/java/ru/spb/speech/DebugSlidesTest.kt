package ru.spb.speech

import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.runner.AndroidJUnit4
import ru.spb.speech.DBTables.SpeechDataBase
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugSlidesTest {
    @Rule
    @JvmField
    var mIntentsTestRule = IntentsTestRule<StartPageActivity>(StartPageActivity::class.java)
    init {
        grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        grantPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        grantPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    @Test
    fun Test(){
        val db = SpeechDataBase.getInstance(getTargetContext())?.PresentationDataDao()
        db?.deleteAll() // удаление всех элементов БД
        assertEquals(db?.getAll()?.size?.toFloat(), 0f) // проверка БД на пустоту
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getTargetContext())
        val debSl = sharedPreferences.edit()
        val OnMode = mIntentsTestRule.activity.getString(R.string.deb_pres)
        val PresName = mIntentsTestRule.activity.getString(R.string.deb_pres_name)
        debSl.putBoolean(OnMode, true)
        debSl.apply()
        onView(withId(R.id.addBtn)).perform(ViewActions.click())
        onView(withText(PresName.substring(0, PresName.indexOf(".pdf")))).check(matches(isDisplayed()))
        onView(withText("26")).check(matches(isDisplayed()))
        onView(withId(R.id.addPresentation)).perform(ViewActions.click())
        debSl.putBoolean(OnMode, false)
        debSl.apply()
    }


}