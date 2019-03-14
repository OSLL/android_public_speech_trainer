package ru.spb.speech

import kotlin.math.abs

fun getAverageSpeed(presentationEntries: HashMap<Int,Float>):Double{
    if (presentationEntries.isEmpty()) return 0.0
    return presentationEntries.values.average()
}

fun getBestSlide(presentationEntries: HashMap<Int,Float>, optimalSpeed: Int):Int{
    if (presentationEntries.isEmpty()) return -1
    // Отсчет слайдов для пользователей идет с единицы
    return presentationEntries.minBy { abs(it.value.minus(optimalSpeed)) }!!.key + 1
}

fun getWorstSlide(presentationEntries: HashMap<Int,Float>, optimalSpeed: Int):Int{
    if (presentationEntries.isEmpty()) return -1
    // Отсчет слайдов для пользователей идет с единицы
    return presentationEntries.maxBy { abs(it.value.minus(optimalSpeed)) }!!.key + 1
}