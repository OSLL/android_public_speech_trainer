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

    fun addDebugPresentation(presentationName: String = "", notifications: Boolean = false): String {
        var name = presentationName
        if (name == "")
            name = activity.getString(R.string.deb_pres_name).split(".")[0]

        Espresso.onView(ViewMatchers.withId(R.id.addBtn)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.presentationName)).perform(ViewActions.clearText(), ViewActions.typeText(name), ViewActions.closeSoftKeyboard())
        if (notifications == true)
            Espresso.onView(ViewMatchers.withId(R.id.notifications)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.addPresentation)).perform(ViewActions.click())

        return name
    }
}