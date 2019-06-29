package ru.spb.speech.appSupport


fun Double.toDefaultStringFormat() = String.format("%.02f", this)

fun Float.format(digits: Int) = String.format("%.${digits}f", this)