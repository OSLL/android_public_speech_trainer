package ru.spb.speech

import org.junit.After
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*

class TextStatisticsTest {

    lateinit var presentationEntries: HashMap<Int, Float>

    @Before
    fun setUp() {
        presentationEntries = HashMap<Int, Float>()
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
        assertEquals(-1, getBestSlide(presentationEntries))
    }

    @Test
    fun getBestSlideOne() {
        presentationEntries[0] = 1f
        assertEquals(0, getBestSlide(presentationEntries))
    }

    @Test
    fun getBestSlideTwo() {
        presentationEntries[0] = 1f
        presentationEntries[1] = 2f
        assertEquals(1, getBestSlide(presentationEntries))
    }

    @Test
    fun getWorstSlideEmpty() {
        assertEquals(-1, getWorstSlide(presentationEntries))
    }

    @Test
    fun getWorstSlideOne() {
        presentationEntries[0] = 1f
        assertEquals(0, getWorstSlide(presentationEntries))
    }

    @Test
    fun getWorstSlideTwo() {
        presentationEntries[0] = 1f
        presentationEntries[1] = 2f
        assertEquals(0, getWorstSlide(presentationEntries))
    }
}