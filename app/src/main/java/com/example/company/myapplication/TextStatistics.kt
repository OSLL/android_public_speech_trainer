package com.example.company.myapplication

import kotlin.math.abs

const val OPTIMAL_SPEED:Int = 120

fun getAverageSpeed(presentationEntries: HashMap<Int,Float>):Double{
    if (presentationEntries.isEmpty()) return 0.0
    return presentationEntries.values.average()
}

fun getBestSlide(presentationEntries: HashMap<Int,Float>):Int{
    if (presentationEntries.isEmpty()) return -1
    // Отсчет слайдов для пользователей идет с единицы
    return presentationEntries.minBy { abs(it.value.minus(OPTIMAL_SPEED)) }!!.key + 1
}

fun getWorstSlide(presentationEntries: HashMap<Int,Float>):Int{
    if (presentationEntries.isEmpty()) return -1
    // Отсчет слайдов для пользователей идет с единицы
    return presentationEntries.maxBy { abs(it.value.minus(OPTIMAL_SPEED)) }!!.key + 1
}