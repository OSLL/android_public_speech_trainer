package ru.spb.speech.DBTables

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.migration.Migration


val MIGRATION_1_2 = object: Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE PresentationData ADD COLUMN notifications INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object: Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE TrainingData ADD COLUMN shareOfParasiticWords STRING NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE TrainingData ADD COLUMN audioPath STRING NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE TrainingData ADD COLUMN audioFileName STRING NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE TrainingData ADD COLUMN trainingGrade STRING DEFAULT NULL")
        database.execSQL("ALTER TABLE TrainingData ADD COLUMN exerciseTimeFactorMarkX STRING DEFAULT NULL")
        database.execSQL("ALTER TABLE TrainingData ADD COLUMN speechSpeedFactorMarkY STRING DEFAULT NULL")
        database.execSQL("ALTER TABLE TrainingData ADD COLUMN timeOnSlidesFactorMarkZ STRING DEFAULT NULL")
    }
}