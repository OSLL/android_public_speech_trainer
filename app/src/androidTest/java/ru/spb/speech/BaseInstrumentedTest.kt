package ru.spb.speech

open class BaseInstrumentedTest {
    init {
        grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        grantPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        grantPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}