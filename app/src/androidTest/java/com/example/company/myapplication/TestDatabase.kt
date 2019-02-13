package com.example.company.myapplication

import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.longClick
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.RootMatchers.isDialog
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.runner.AndroidJUnit4
import com.example.putkovdimi.trainspeech.DBTables.SpeechDataBase
import junit.framework.Assert.*
import kotlinx.android.synthetic.main.activity_start_page.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestDatabase {
    @Rule
    @JvmField
    var mControllerTestRule = ControlledActivityTestRule<StartPageActivity>(StartPageActivity::class.java)

    @Before
    fun enableDebugMode() {
        setTrainingPresentationMod(true) // включение тестовой презентации
    }

    @After
    fun disableDebugMode() {
        setTrainingPresentationMod(false) // выключение тестовой презентации
    }

    @Test
    fun addNewPresentationManuallyTest() {
        val db = SpeechDataBase.getInstance(getTargetContext())?.PresentationDataDao()

        db?.deleteAll() // удаление всех элементов БД
        assertEquals(db?.getAll()?.size?.toFloat(), 0f) // проверка БД на пустоту

        mControllerTestRule.relaunchActivity() // перезапуск активити для обновления recyclerView
        assertEquals(mControllerTestRule.activity.recyclerview_startpage.childCount.toFloat(), 0f) // проверка кол-ва элементов в RV

        // Добавление новой презентации
        onView(withId(R.id.addBtn)).perform(click())
        onView(withId(R.id.addPresentation)).perform(click())

        assertEquals(db?.getAll()?.size?.toFloat(), 1f) // проверка на добавление нового эл-та в БД
        assertEquals(mControllerTestRule.activity.recyclerview_startpage.childCount.toFloat(), 1f) // проверка добавление эл-та в RV
    }

    @Test
    fun addDuplicatePresentationTest() {
        val db = SpeechDataBase.getInstance(getTargetContext())?.PresentationDataDao()

        db?.deleteAll() // удаление всех элементов БД
        assertEquals(db?.getAll()?.size?.toFloat(), 0f) // проверка БД на пустоту

        mControllerTestRule.relaunchActivity() // перезапуск активити для обновления recyclerView
        assertEquals(mControllerTestRule.activity.recyclerview_startpage.childCount.toFloat(), 0f) // проверка кол-ва элементов в RV

        // Добавление новой презентации
        for (i in 0..1) {
            onView(withId(R.id.addBtn)).perform(click())
            onView(withId(R.id.addPresentation)).perform(click())
        }

        assertEquals(db?.getAll()?.size?.toFloat(), 1f) // проверка на добавление нового эл-та в БД
        assertEquals(mControllerTestRule.activity.recyclerview_startpage.childCount.toFloat(), 1f) // проверка добавление эл-та в RV
    }

    @Test
    fun removePresentationTest() {
        val db = SpeechDataBase.getInstance(getTargetContext())?.PresentationDataDao()

        db?.deleteAll() // удаление всех элементов БД
        assertEquals(db?.getAll()?.size?.toFloat(), 0f) // проверка БД на пустоту

        mControllerTestRule.relaunchActivity() // перезапуск активити для обновления recyclerView
        assertEquals(mControllerTestRule.activity.recyclerview_startpage.childCount.toFloat(), 0f) // проверка кол-ва элементов в RV

        // Добавление новой презентации
        onView(withId(R.id.addBtn)).perform(click())
        onView(withId(R.id.addPresentation)).perform(click())

        assertEquals(db?.getAll()?.size?.toFloat(), 1f) // проверка на добавление нового эл-та в БД
        assertEquals(mControllerTestRule.activity.recyclerview_startpage.childCount.toFloat(), 1f) // проверка добавление эл-та в RV

        onView(withId(R.id.image_view_presentation_start_page_row)).perform(longClick()) // Вызов диалогового окна удаления презентации

        // Нажатие на кнопку "удалить"
        onView(withText(mControllerTestRule.activity.getString(R.string.remove)))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
                .perform(click())
        assertEquals(mControllerTestRule.activity.recyclerview_startpage.childCount.toFloat(), 0f) // проверка кол-ва элементов в RV
        //assertEquals(db?.getAll()?.size?.toFloat(), 0f) // проверка БД на пустоту
    }

    private fun setTrainingPresentationMod(mode: Boolean) {
        val sp = PreferenceManager.getDefaultSharedPreferences(getTargetContext())
        val spe = sp.edit()
        val testPresentationMode = mControllerTestRule.activity.getString(R.string.deb_pres)

        spe.putBoolean(testPresentationMode, mode)
        spe.apply()
    }
}