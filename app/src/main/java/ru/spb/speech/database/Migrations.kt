package ru.spb.speech.DBTables

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.migration.Migration

val MIGRATION_1_2 = object: Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE PresentationData ADD COLUMN notifications INTEGER NOT NULL DEFAULT 0")
    }
}