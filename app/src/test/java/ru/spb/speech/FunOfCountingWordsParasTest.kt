package ru.spb.speech

import android.content.Context
import org.junit.Assert
import org.junit.Test
import ru.spb.speech.appSupport.CountingNumberOfWordsParasites
import org.junit.Before
import android.support.test.InstrumentationRegistry







class FunOfCountingWordsParasTest {

    private val countingParasitesHelper = CountingNumberOfWordsParasites()

    private val massOfParWords = arrayOf("короче", "однако", "это", "типа")

    @Test
    fun test1() {
        Assert.assertEquals(countingParasitesHelper.counting("однако", massOfParWords), 1)
    }

    @Test
    fun test2() {
        Assert.assertEquals(countingParasitesHelper.counting("короче, было проведено исследование типа", massOfParWords), 2)
    }

    @Test
    fun test3() {
        Assert.assertEquals(countingParasitesHelper.counting("было проведено исследование", massOfParWords), 0)
    }
}

