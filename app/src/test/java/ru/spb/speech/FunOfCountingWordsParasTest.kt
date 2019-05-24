package ru.spb.speech

import org.junit.Assert
import org.junit.Test
import ru.spb.speech.appSupport.CountingNumberOfWordsParasites

class FunOfCountingWordsParasTest {
    private val countingParasitesHelper = CountingNumberOfWordsParasites()
    private val massOfParWords = arrayOf("короче", "однако", "это", "типа")

    @Test
    fun oneWordParasiteTest() {
        Assert.assertEquals(countingParasitesHelper.counting("однако", massOfParWords), 1)
    }

    @Test
    fun twoWordParasiteTest() {
        Assert.assertEquals(countingParasitesHelper.counting("короче, было проведено исследование типа", massOfParWords), 2)
    }

    @Test
    fun zeroWordParasitesTest() {
        Assert.assertEquals(countingParasitesHelper.counting("было проведено исследование", massOfParWords), 0)
    }
}

