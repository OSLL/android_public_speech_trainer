package ru.spb.speech

import android.app.Activity
import android.support.test.espresso.Espresso
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.matcher.RootMatchers
import android.support.test.espresso.matcher.ViewMatchers

fun removeDebugSlides(activity: Activity){
    Espresso.onView(ViewMatchers.withId(R.id.image_view_presentation_start_page_row)).
            perform(ViewActions.longClick()) // Вызов диалогового окна удаления презентации

    // Нажатие на кнопку "удалить"
    Espresso.onView(ViewMatchers.withText(activity.getString(R.string.remove)))
            .inRoot(RootMatchers.isDialog())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .perform(ViewActions.click())
}