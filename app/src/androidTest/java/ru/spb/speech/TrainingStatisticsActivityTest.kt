package ru.spb.speech

import android.content.Intent
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.runner.AndroidJUnit4
import android.widget.TextView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.spb.speech.DBTables.PresentationData
import ru.spb.speech.DBTables.SpeechDataBase
import ru.spb.speech.DBTables.TrainingData
import ru.spb.speech.DBTables.TrainingSlideData
import ru.spb.speech.DBTables.helpers.TrainingDBHelper
import ru.spb.speech.DBTables.helpers.TrainingSlideDBHelper
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.Espresso.*
import android.support.test.espresso.action.ViewActions.*
import ru.spb.speech.TrainingStatisticsActivityTest.TestConstants.allText
import ru.spb.speech.TrainingStatisticsActivityTest.TestConstants.firstSlideText
import ru.spb.speech.TrainingStatisticsActivityTest.TestConstants.s1Speed
import ru.spb.speech.TrainingStatisticsActivityTest.TestConstants.s2Speed
import ru.spb.speech.TrainingStatisticsActivityTest.TestConstants.secondSlideText
import ru.spb.speech.TrainingStatisticsActivityTest.TestConstants.slide1spentTime
import ru.spb.speech.TrainingStatisticsActivityTest.TestConstants.slide2spentTime
import java.util.*
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class TrainingStatisticsActivityTest {
    @Rule
    @JvmField
    var mControllerTestRule = ControlledActivityTestRule<TrainingStatisticsActivity>(TrainingStatisticsActivity::class.java)

    private val tContext = InstrumentationRegistry.getTargetContext()
    private val db: SpeechDataBase = SpeechDataBase.getInstance(tContext)!!

    private val presId: Int
    private val trainingId: Int

    /**
     * Удаление всех тестовых презентаций из БД
     * Добавление новой тестовой презентации в БД
     */
    init {
        db.PresentationDataDao().deleteTestPresentations()
        val list = addDummyPresentation()
        presId = list[0]
        trainingId = list[1]
    }


    @Before
    fun before() {}

    @After
    fun after() {
        db.PresentationDataDao().deleteTestPresentations()
    }


    /**
     * Проверка корректности отображения всех view
     */
    @Test
    fun checkViews() {
        changeActivityIntent()

        onView(withId(R.id.earnOfTrain)).perform(scrollTo())
        onView(withId(R.id.earnOfTrain)).check(matches(isDisplayed()))

        onView(withId(R.id.x_exercise_time_factor)).check(matches(isDisplayed()))
        onView(withId(R.id.y_speech_speed_factor)).check(matches(isDisplayed()))
        onView(withId(R.id.z_time_on_slides_factor)).check(matches(isDisplayed()))

        onView(withId(R.id.speed_bar_chart)).perform(scrollTo())
        onView(withId(R.id.speed_bar_chart)).check(matches(isDisplayed()))
        onView(withId(R.id.time_on_each_slide_chart)).check(matches(isDisplayed()))

        onView(withId(R.id.pie_chart)).perform(scrollTo())
        onView(withId(R.id.pie_chart)).check(matches(isDisplayed()))

        onView(withId(R.id.returnTraining)).perform(scrollTo()).check(matches(isClickable())).check(matches(isDisplayed()))
        onView(withId(R.id.export)).check(matches(isClickable())).check(matches(isDisplayed()))
        onView(withId(R.id.share1)).check(matches(isClickable())).check(matches(isDisplayed()))

        assertTrue(!mControllerTestRule.activity.findViewById<TextView>(R.id.x_exercise_time_factor).text.isNullOrEmpty())
        assertTrue(!mControllerTestRule.activity.findViewById<TextView>(R.id.y_speech_speed_factor).text.isNullOrEmpty())
        assertTrue(!mControllerTestRule.activity.findViewById<TextView>(R.id.z_time_on_slides_factor).text.isNullOrEmpty())
    }


    /**
     * Проверка текстовой статистики
     */
    @Test
    fun checkTestStatistics() {
        changeActivityIntent()

        val textStatistics = mControllerTestRule.activity.findViewById<TextView>(R.id.textView).text.toString()

        assertTrue(textStatistics.contains(tContext.getString(R.string.average_speed) +
                " %.2f ${tContext.getString(R.string.speech_speed_units)}\n".format(listOf(s1Speed, s2Speed).average())))

        assertTrue(textStatistics.contains("${tContext.getString(R.string.training_time)} ${formatTime(slide1spentTime + slide2spentTime)}"))
    }


    /**
     * Проверка графика 'Количество слов в минуту'
     */
    @Test
    fun testSpeedChart() {
        changeActivityIntent()

        val speedChart = mControllerTestRule.activity.findViewById<BarChart>(R.id.speed_bar_chart)
        assertEquals(speedChart.barData.entryCount, 2)
        assertEquals(speedChart.barData.dataSets[0].getEntryForIndex(0).x, 0f)
        assertEquals(speedChart.barData.dataSets[0].getEntryForIndex(0).y, s1Speed)
        assertEquals(speedChart.barData.dataSets[0].getEntryForIndex(1).x, 1f)
        assertEquals(speedChart.barData.dataSets[0].getEntryForIndex(1).y, s2Speed)
        assertEquals(speedChart.barData.dataSets[0].label, tContext.getString(R.string.words_count))
        assertEquals(speedChart.description.text, tContext.getString(R.string.slide_number))
    }


    /**
     * Проверка графика 'Длительность слайда'
     */
    @Test
    fun testTimeChart() {
        changeActivityIntent()

        val slidesTime = mControllerTestRule.activity.findViewById<BarChart>(R.id.time_on_each_slide_chart)
        assertEquals(slidesTime.barData.entryCount, 2)
        assertEquals(slidesTime.barData.dataSets[0].getEntryForIndex(0).x, 0f)
        assertEquals(slidesTime.barData.dataSets[0].getEntryForIndex(0).y, slide1spentTime.toFloat())
        assertEquals(slidesTime.barData.dataSets[0].getEntryForIndex(1).x, 1f)
        assertEquals(slidesTime.barData.dataSets[0].getEntryForIndex(1).y, slide2spentTime.toFloat())
        assertEquals(slidesTime.barData.dataSets[0].label, tContext.getString(R.string.slideDurationInSeconds))
        assertEquals(slidesTime.description.text, tContext.getString(R.string.slide_number))
    }


    /**
     * Проверка графика 'Наиболее популярные слова'
     */
    @Test
    fun testPieChart() {
        changeActivityIntent()

        val pieChart = mControllerTestRule.activity.findViewById<PieChart>(R.id.pie_chart)
        assertEquals(pieChart.data.dataSets[0].entryCount, 10)
        assertEquals(pieChart.centerText, tContext.getString(R.string.pie_chart_tittle))
    }


    /**
     * Функция создает тестовую презентацию с 1 тренировкой и 2 слайдами, добавляет ее в БД
     * Возращает [0] - id Презентации, [1] - id тренировки в БД
     */
    private fun addDummyPresentation(): List<Int> {
        var presentationData = PresentationData()
        presentationData.debugFlag = 1
        presentationData.name = tContext.getString(R.string.deb_pres_name)
        presentationData.pageCount = 2
        presentationData.timeLimit = 120
        presentationData.stringUri =  tContext.getString(R.string.deb_pres_name)
        db.PresentationDataDao().insert(presentationData)
        presentationData = db.PresentationDataDao().getLastPresentation()

        val trainingHelper = TrainingDBHelper(tContext)
        val slidesHelper = TrainingSlideDBHelper(tContext)

        var training1 = TrainingData()
        training1.allRecognizedText = allText
        training1.timeStampInSec = slide1spentTime + slide2spentTime

        trainingHelper.addTrainingInDB(training1, presentationData)
        training1 = db.TrainingDataDao().getLastTraining()

        val slide1 = TrainingSlideData()
        slide1.knownWords = firstSlideText
        slide1.spentTimeInSec = slide1spentTime

        val slide2 = TrainingSlideData()
        slide2.knownWords = secondSlideText
        slide2.spentTimeInSec = slide2spentTime

        slidesHelper.addTrainingSlideInDB(slide1, training1)
        slidesHelper.addTrainingSlideInDB(slide2, training1)

        return listOf(presentationData.id!!, training1.id!!)
    }


    /**
     * Обновление активти с добавлением интета храняещего иформацию о тестовой перезентации и
     * тренировке
     */
    private fun changeActivityIntent() {
        val intent = Intent(mControllerTestRule.activity, TrainingStatisticsActivity::class.java)
        intent.putExtra(tContext.getString(R.string.CURRENT_PRESENTATION_ID), presId)
        intent.putExtra(tContext.getString(R.string.CURRENT_TRAINING_ID), trainingId)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

        mControllerTestRule.launchActivity(intent)
    }

    private object TestConstants {
        const val slide1spentTime: Long = 5
        const val slide2spentTime: Long = 6
        const val firstSlideText = "1 2 3 4 5 6 7 8 9"
        const val secondSlideText = "a b c d e f g"

        const val allText = "$firstSlideText $secondSlideText"
        val s1Speed = firstSlideText.split(" ").size.toFloat() / slide1spentTime.toFloat() * 60f
        val s2Speed = secondSlideText.split(" ").size.toFloat() / slide2spentTime.toFloat() * 60f
    }

    private fun formatTime(t: Long)
            = String.format(Locale.getDefault(), " %02d:%02d", TimeUnit.SECONDS.toMinutes(t), t % 60)
}