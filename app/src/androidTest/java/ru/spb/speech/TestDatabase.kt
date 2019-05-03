package ru.spb.speech

import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.runner.AndroidJUnit4
import ru.spb.speech.database.SpeechDataBase
import junit.framework.Assert.*
import kotlinx.android.synthetic.main.activity_start_page.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestDatabase : BaseInstrumentedTest() {
    @Rule
    @JvmField
    var mControllerTestRule = ControlledActivityTestRule<StartPageActivity>(StartPageActivity::class.java)

    lateinit var helper: TestHelper
    private val db: SpeechDataBase
    private val startDbSize: Float

    init {
        db = SpeechDataBase.getInstance(getTargetContext())!!
        db.PresentationDataDao().deleteTestPresentations()
        startDbSize = db.PresentationDataDao().getAll().size.toFloat()
    }

    @Before
    fun enableDebugMode() {
        helper = TestHelper(mControllerTestRule.activity)
        helper.setTrainingPresentationMod(true) // включение тестовой презентации
        mControllerTestRule.relaunchActivity()
    }

    @After
    fun disableDebugMode() {
        helper.setTrainingPresentationMod(false) // выключение тестовой презентации
        db.PresentationDataDao().deleteTestPresentations()
    }

    @Test
    fun addNewPresentationManuallyTest() {
        assertEquals(mControllerTestRule.activity.recyclerview_startpage.childCount.toFloat(), startDbSize) // проверка кол-ва элементов в RV

        // Добавление новой презентации
        onView(withId(R.id.addBtn)).perform(click())
        onView(withId(R.id.addPresentation)).perform(click())

        Thread.sleep(2000) // ожидание асинхронного сохраенения первого слайда в БД

        assertEquals(db.PresentationDataDao().getAll().size.toFloat(), startDbSize + 1f) // проверка на добавление нового эл-та в БД
        assertEquals(mControllerTestRule.activity.recyclerview_startpage.childCount.toFloat(), startDbSize + 1f) // проверка добавление эл-та в RV
    }

    @Test
    fun addDuplicatePresentationTest() {
        assertEquals(mControllerTestRule.activity.recyclerview_startpage.childCount.toFloat(), startDbSize) // проверка кол-ва элементов в RV

        // Добавление новой презентации
        for (i in 0..1) {
            onView(withId(R.id.addBtn)).perform(click())
            onView(withId(R.id.addPresentation)).perform(click())
            Thread.sleep(2000)
        }

        assertEquals(db.PresentationDataDao().getAll().size.toFloat(), startDbSize + 1f) // проверка на добавление нового эл-та в БД
        assertEquals(mControllerTestRule.activity.recyclerview_startpage.childCount.toFloat(), startDbSize + 1f) // проверка добавление эл-та в RV
    }

    @Test
    fun removePresentationTest() {
        assertEquals(mControllerTestRule.activity.recyclerview_startpage.childCount.toFloat(), startDbSize) // проверка кол-ва элементов в RV

        // Добавление новой презентации
        onView(withId(R.id.addBtn)).perform(click())
        onView(withId(R.id.addPresentation)).perform(click())

        Thread.sleep(2000) // ожидание асинхронного сохраенения первого слайда в БД

        assertEquals(db.PresentationDataDao().getAll().size.toFloat(), startDbSize + 1f) // проверка на добавление нового эл-та в БД
        assertEquals(mControllerTestRule.activity.recyclerview_startpage.childCount.toFloat(), startDbSize + 1f) // проверка добавление эл-та в RV

        val testPres = db.PresentationDataDao().getLastPresentation()
        assertNotNull(testPres.id)

        // Добавление тестовой статистики для презентации
        val response = helper.addDummyStatisticsForPresentation(testPres.id!!, getTargetContext())
        assertNotNull(response)

        // Проверка на наличие статистики в БД
        for (training in response!!.trainings)
            assertNotNull(db.TrainingDataDao().getTrainingWithId(training.id!!))

        for (slide in response.slides)
            assertNotNull(db.TrainingSlideDataDao().getSlideWithId(slide.id!!))

        // Удаление тестовой презентации
        helper.removePresentationFromRecyclerView(startDbSize.toInt())

        // Проверка на удаление статистики презентации из БД
        for (training in response.trainings)
            assertNull(db.TrainingDataDao().getTrainingWithId(training.id!!))

        for (slide in response.slides)
            assertNull(db.TrainingSlideDataDao().getSlideWithId(slide.id!!))

        assertEquals(mControllerTestRule.activity.recyclerview_startpage.childCount.toFloat(), startDbSize) // проверка кол-ва элементов в RV
        assertEquals(db.PresentationDataDao().getAll().size.toFloat(), startDbSize) // проверка БД на пустоту
    }
}