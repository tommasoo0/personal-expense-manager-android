package com.expenses.mobile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        AccountEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun referenceDao(): ReferenceDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createReferenceTables(db)
                seedReferenceTables(db)
            }
        }

        private val seedOnCreate = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                seedReferenceTables(db)
            }
        }

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gestore-spese.db"
                )
                    .addMigrations(migration1To2)
                    .addCallback(seedOnCreate)
                    .build()
                    .also { instance = it }
            }
        }

        private fun createReferenceTables(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS categories (name TEXT NOT NULL PRIMARY KEY)")
            db.execSQL("CREATE TABLE IF NOT EXISTS accounts (name TEXT NOT NULL PRIMARY KEY)")
        }

        private fun seedReferenceTables(db: SupportSQLiteDatabase) {
            createReferenceTables(db)

            listOf("Cibo", "Trasporti", "Casa", "Salute", "Intrattenimento", "Stipendio", "Altro")
                .forEach { db.execSQL("INSERT OR IGNORE INTO categories(name) VALUES (?)", arrayOf(it)) }
            listOf("Contanti", "Conto Bancario")
                .forEach { db.execSQL("INSERT OR IGNORE INTO accounts(name) VALUES (?)", arrayOf(it)) }

            db.execSQL(
                """
                INSERT OR IGNORE INTO categories(name)
                SELECT DISTINCT category FROM transactions WHERE trim(category) <> ''
                """
            )
            db.execSQL(
                """
                INSERT OR IGNORE INTO accounts(name)
                SELECT DISTINCT account FROM transactions WHERE trim(account) <> ''
                """
            )
        }
    }
}
