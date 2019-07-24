package ru.spb.speech.appSupport

import android.support.v7.app.AppCompatActivity
import ru.spb.speech.fragments.StatisticsFragment


fun Double.toDefaultStringFormat() = String.format("%.02f", this)

fun Float.format(digits: Int) = String.format("%.${digits}f", this)

fun AppCompatActivity.showStatisticsFragment(f: StatisticsFragment, container: Int, trainingId: Int)
        = supportFragmentManager
        .beginTransaction()
        .replace(container, StatisticsFragment.instance(f, trainingId), f::class.java.name)
        .commit()

fun AppCompatActivity.showStatisticsFragments(
        vararg fragmentContainerPairs: Pair<StatisticsFragment,
        Int>, trainingId: Int) = fragmentContainerPairs.forEach {
    showStatisticsFragment(it.first, it.second, trainingId)
}