package ru.spb.speech

import android.support.test.runner.permission.PermissionRequester

fun grantPermissions(vararg permissions: String) {
    PermissionRequester().apply {
        addPermissions(*permissions)
        requestPermissions()
    }
}