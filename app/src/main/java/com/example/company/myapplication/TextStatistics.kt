package com.example.company.myapplication

import kotlin.math.abs

fun getAverageSpeed(presentationEntries: HashMap<Int,Float>):Double{
    if (presentationEntries.isEmpty()) return 0.0
    return presentationEntries.values.average()
}

fun getBestSlide(presentationEntries: HashMap<Int,Float>):Int{
    if (presentationEntries.isEmpty()) return -1
    return presentationEntries.minBy { abs(it.value.minus(120)) }!!.key
}

fun getWorstSlide(presentationEntries: HashMap<Int,Float>):Int{
    if (presentationEntries.isEmpty()) return -1
    return presentationEntries.maxBy { abs(it.value.minus(120)) }!!.key
}