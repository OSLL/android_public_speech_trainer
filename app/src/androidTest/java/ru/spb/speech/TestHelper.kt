package ru.spb.speech

import android.app.Activity
import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.matcher.RootMatchers
import android.support.test.espresso.matcher.ViewMatchers

class TestHelper(private val activity: Activity) {

    fun setTrainingPresentationMod(mode: Boolean) {
        val sp = PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getTargetContext())
        val spe = sp.edit()
        val testPresentationMode = activity.getString(R.string.deb_pres)

        spe.putBoolean(testPresentationMode, mode)
        spe.apply()
    }

    fun removeDebugSlides(){
        Espresso.onView(ViewMatchers.withId(R.id.image_view_presentation_start_page_row)).
                perform(ViewActions.longClick()) // Вызов диалогового окна удаления презентации

        // Нажатие на кнопку "удалить"
        Espresso.onView(ViewMatchers.withText(activity.getString(R.string.remove)))
                .inRoot(RootMatchers.isDialog())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click())
    }
}