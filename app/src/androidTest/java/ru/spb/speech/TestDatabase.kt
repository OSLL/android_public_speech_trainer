package ru.spb.speech

import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.longClick
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.RootMatchers.isDialog
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.runner.AndroidJUnit4
import ru.spb.speech.DBTables.SpeechDataBase
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

    init {
        grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        grantPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        grantPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    lateinit var helper: TestHelper

    @Before
    fun enableDebugMode() {
        helper = TestHelper(mControllerTestRule.activity)
        helper.setTrainingPresentationMod(true) // включение тестовой презентации
    }

    @After
    fun disableDebugMode() {
        helper.setTrainingPresentationMod(false) // выключение тестовой презентации
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

        Thread.sleep(2000) // ожидание асинхронного сохраенения первого слайда в БД

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
            Thread.sleep(2000)
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

        Thread.sleep(2000) // ожидание асинхронного сохраенения первого слайда в БД

        assertEquals(db?.getAll()?.size?.toFloat(), 1f) // проверка на добавление нового эл-та в БД
        assertEquals(mControllerTestRule.activity.recyclerview_startpage.childCount.toFloat(), 1f) // проверка добавление эл-та в RV

        helper.removeDebugSlides()

        assertEquals(mControllerTestRule.activity.recyclerview_startpage.childCount.toFloat(), 0f) // проверка кол-ва элементов в RV
        assertEquals(db?.getAll()?.size?.toFloat(), 0f) // проверка БД на пустоту
    }

}