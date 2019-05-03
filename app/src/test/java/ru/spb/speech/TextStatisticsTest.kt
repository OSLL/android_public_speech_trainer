package ru.spb.speech

import org.junit.After
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*
import ru.spb.speech.vocabulary.TextHelper

class TextStatisticsTest {
    private val textHelper = TextHelper(emptyArray())
    lateinit var presentationEntries: HashMap<Int, Float>
    private val optimalSpeed = 120

    @Before
    fun setUp() {
        presentationEntries = HashMap()
    }

    @After
    fun tearDown() {
    }

    @Test
    fun getAverageSpeedEmpty() {
        assertEquals(0.0,getAverageSpeed(presentationEntries), 1e-6)
    }

    @Test
    fun getAverageSpeedOne() {
        presentationEntries[0] = 1f
        assertEquals(1.0,getAverageSpeed(presentationEntries), 1e-6)
    }

    @Test
    fun getAverageSpeedTwo() {
        presentationEntries[0] = 1f
        presentationEntries[1] = 2f
        assertEquals(1.5,getAverageSpeed(presentationEntries), 1e-6)
    }

    @Test
    fun getBestSlideEmpty() {
        assertEquals(-1, getBestSlide(presentationEntries, optimalSpeed))
    }

    @Test
    fun getBestSlideOne() {
        presentationEntries[0] = 1f
        assertEquals(1, getBestSlide(presentationEntries, optimalSpeed))
    }

    @Test
    fun getBestSlideTwo() {
        presentationEntries[0] = 1f
        presentationEntries[1] = 2f
        assertEquals(2, getBestSlide(presentationEntries, optimalSpeed))
    }

    @Test
    fun getWorstSlideEmpty() {
        assertEquals(-1, getWorstSlide(presentationEntries, optimalSpeed))
    }

    @Test
    fun getWorstSlideOne() {
        presentationEntries[0] = 1f
        assertEquals(1, getWorstSlide(presentationEntries, optimalSpeed))
    }

    @Test
    fun getWorstSlideTwo() {
        presentationEntries[0] = 1f
        presentationEntries[1] = 2f
        assertEquals(1, getWorstSlide(presentationEntries, optimalSpeed))
    }

    @Test
    fun testTop10WithEmptyInput() {
        assertEquals(textHelper.getTop10WordsRmConjStemm(""), emptyList<Pair<String, Int>>())
    }

    @Test
    fun testTop10WithSingleWord() {
        assertEquals(textHelper.getTop10WordsRmConjStemm(Top10WordsTestData.Test1.input),
                Top10WordsTestData.Test1.result)
    }

    @Test
    fun testTop10With2Words() {
        assertEquals(textHelper.getTop10WordsRmConjStemm(Top10WordsTestData.Test2.input),
                Top10WordsTestData.Test2.result)
    }

    @Test
    fun testTop10With10DifferentWords() {
        val res = textHelper.getTop10WordsRmConjStemm(Top10WordsTestData.Test3.input)
        assertEquals(res.size, 10)
        assertEquals(Top10WordsTestData.Test3.result.size, 10)
        for (p in res) assertTrue(Top10WordsTestData.Test3.result.contains(p))
    }

    @Test
    fun testTop10With10DifferentWordsAndDuplicates() {
        val res = textHelper.getTop10WordsRmConjStemm(Top10WordsTestData.Test4.input)
        assertEquals(res.size, 10)
        assertEquals(Top10WordsTestData.Test4.result.size, 10)

        assertEquals(res[0].first, Top10WordsTestData.Test2.result.first().first)
        assertEquals(res[0].second, 5)

        assertEquals(res[1].first, Top10WordsTestData.Test2.result.last().first)
        assertEquals(res[1].second, 5)
        
        for (p in res) assertTrue(Top10WordsTestData.Test4.result.contains(p))
    }
}

object Top10WordsTestData {
    object Test1 {
        const val input = "река рекой реку реки"
        val result = listOf(Pair("река", 4))
    }

    object Test2 {
        const val input = "${Test1.input} рука руки рукой рукам"
        val result = Test1.result + listOf(Pair("рука", 4))
    }

    object Test3 {
        const val input = "рука река мост дом дерево лист окно забор лампа колесо"
        val result: List<Pair<String, Int>> get() {
            val list =  ArrayList<Pair<String, Int>>()
            for (w in input.split(" ")) list.add(Pair(w, 1))
            return list
        }
    }

    object Test4 {
        const val input = "${Test2.input} ${Test3.input}"
        val result = listOf(Pair("река", 5), Pair("рука", 5),
                Pair("мост", 1), Pair("дом", 1), Pair("дерево", 1), Pair("лист", 1),
                Pair("окно", 1), Pair("забор", 1), Pair("лампа", 1), Pair("колесо", 1))
    }
}