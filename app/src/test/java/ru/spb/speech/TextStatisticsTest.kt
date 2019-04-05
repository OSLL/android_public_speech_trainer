package ru.spb.speech

import org.junit.After
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*

class TextStatisticsTest {


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

}