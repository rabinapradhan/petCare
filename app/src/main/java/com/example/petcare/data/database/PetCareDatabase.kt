package com.example.petcare.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

import com.example.petcare.data.dao.ExpenseDao
import com.example.petcare.data.dao.PetDao
import com.example.petcare.data.dao.PlaceDao
import com.example.petcare.data.dao.TaskDao
import com.example.petcare.data.dao.UserDao

import com.example.petcare.data.entity.Expense
import com.example.petcare.data.entity.Pet
import com.example.petcare.data.entity.Place
import com.example.petcare.data.entity.Task
import com.example.petcare.data.entity.User

@Database(
    entities = [
        User::class,
        Pet::class,
        Task::class,
        Place::class,
        Expense::class
    ],
    version = 7,
    exportSchema = false
)
abstract class PetCareDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun petDao(): PetDao

    abstract fun taskDao(): TaskDao

    abstract fun placeDao(): PlaceDao

    abstract fun expenseDao(): ExpenseDao

    companion object {

        @Volatile
        private var INSTANCE: PetCareDatabase? = null


        // Migration 1 -> 2
        // Adds pets table

        private val MIGRATION_1_2 = object : Migration(1, 2) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {

                database.execSQL(
                    """
                    CREATE TABLE pets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        species TEXT NOT NULL,
                        breed TEXT NOT NULL,
                        age TEXT NOT NULL,
                        weight TEXT NOT NULL,
                        dietaryPreferences TEXT NOT NULL,
                        allergies TEXT NOT NULL,
                        vaccinationHistory TEXT NOT NULL,
                        favouriteToys TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        photoUri TEXT,
                        FOREIGN KEY(userId)
                            REFERENCES users(id)
                            ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX index_pets_userId
                    ON pets(userId)
                    """.trimIndent()
                )
            }
        }


        // Migration 2 -> 3
        // Adds tasks table

        private val MIGRATION_2_3 = object : Migration(2, 3) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {

                database.execSQL(
                    """
                    CREATE TABLE tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        petId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        category TEXT NOT NULL,
                        repeats TEXT NOT NULL,
                        time TEXT NOT NULL,
                        supplies TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        completed INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX index_tasks_userId
                    ON tasks(userId)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX index_tasks_petId
                    ON tasks(petId)
                    """.trimIndent()
                )
            }
        }

        // Migration 3 -> 4
        // Adds places table

        private val MIGRATION_3_4 = object : Migration(3, 4) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {

                database.execSQL(
                    """
                    CREATE TABLE places (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        googlePlaceId TEXT,
                        name TEXT NOT NULL,
                        address TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        phone TEXT NOT NULL,
                        openingHours TEXT NOT NULL,
                        type TEXT NOT NULL,
                        FOREIGN KEY(userId)
                            REFERENCES users(id)
                            ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX index_places_userId
                    ON places(userId)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX index_places_googlePlaceId
                    ON places(googlePlaceId)
                    """.trimIndent()
                )
            }
        }


        // Migration 4 -> 5
        // Adds expenses table

        private val MIGRATION_4_5 = object : Migration(4, 5) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {

                database.execSQL(
                    """
                    CREATE TABLE expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        petId INTEGER,
                        amount REAL NOT NULL,
                        category TEXT NOT NULL,
                        date TEXT NOT NULL,
                        note TEXT NOT NULL,
                        FOREIGN KEY(userId)
                            REFERENCES users(id)
                            ON DELETE CASCADE,
                        FOREIGN KEY(petId)
                            REFERENCES pets(id)
                            ON DELETE SET NULL
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX index_expenses_userId
                    ON expenses(userId)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX index_expenses_petId
                    ON expenses(petId)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX index_expenses_date
                    ON expenses(date)
                    """.trimIndent()
                )
            }
        }

        // Migration 5 -> 6
        // Adds startDate and completedDate to tasks

        private val MIGRATION_5_6 =
            object : Migration(5, 6) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        ALTER TABLE tasks
                        ADD COLUMN startDate TEXT NOT NULL
                        DEFAULT '2026-01-01'
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE tasks
                        ADD COLUMN completedDate TEXT
                        """.trimIndent()
                    )
                }
            }


        // Migration 6 -> 7
        //
        // Adds explicit foreign-key constraints to:
        //
        // Task.userId -> User.id
        // Task.petId  -> Pet.id
        // Place.userId -> User.id
        //
        // SQLite does not support directly adding a foreign key
        // to an existing table, so the affected tables are
        // recreated while preserving their existing data.

        private val MIGRATION_6_7 =
            object : Migration(6, 7) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {


                    // Recreate TASKS table

                    database.execSQL(
                        """
                        CREATE TABLE tasks_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            userId INTEGER NOT NULL,
                            petId INTEGER NOT NULL,
                            title TEXT NOT NULL,
                            category TEXT NOT NULL,
                            repeats TEXT NOT NULL,
                            time TEXT NOT NULL,
                            supplies TEXT NOT NULL,
                            notes TEXT NOT NULL,
                            completed INTEGER NOT NULL DEFAULT 0,
                            startDate TEXT NOT NULL,
                            completedDate TEXT,
                            FOREIGN KEY(userId)
                                REFERENCES users(id)
                                ON DELETE CASCADE,
                            FOREIGN KEY(petId)
                                REFERENCES pets(id)
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    // Copy existing task records.
                    database.execSQL(
                        """
                        INSERT INTO tasks_new (
                            id,
                            userId,
                            petId,
                            title,
                            category,
                            repeats,
                            time,
                            supplies,
                            notes,
                            completed,
                            startDate,
                            completedDate
                        )
                        SELECT
                            id,
                            userId,
                            petId,
                            title,
                            category,
                            repeats,
                            time,
                            supplies,
                            notes,
                            completed,
                            startDate,
                            completedDate
                        FROM tasks
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        DROP TABLE tasks
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE tasks_new
                        RENAME TO tasks
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX index_tasks_userId
                        ON tasks(userId)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX index_tasks_petId
                        ON tasks(petId)
                        """.trimIndent()
                    )


                    // Recreate PLACES table

                    database.execSQL(
                        """
                        CREATE TABLE places_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            userId INTEGER NOT NULL,
                            googlePlaceId TEXT,
                            name TEXT NOT NULL,
                            address TEXT NOT NULL,
                            latitude REAL NOT NULL,
                            longitude REAL NOT NULL,
                            phone TEXT NOT NULL,
                            openingHours TEXT NOT NULL,
                            type TEXT NOT NULL,
                            FOREIGN KEY(userId)
                                REFERENCES users(id)
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    // Copy existing place records.
                    database.execSQL(
                        """
                        INSERT INTO places_new (
                            id,
                            userId,
                            googlePlaceId,
                            name,
                            address,
                            latitude,
                            longitude,
                            phone,
                            openingHours,
                            type
                        )
                        SELECT
                            id,
                            userId,
                            googlePlaceId,
                            name,
                            address,
                            latitude,
                            longitude,
                            phone,
                            openingHours,
                            type
                        FROM places
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        DROP TABLE places
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE places_new
                        RENAME TO places
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX index_places_userId
                        ON places(userId)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX index_places_googlePlaceId
                        ON places(googlePlaceId)
                        """.trimIndent()
                    )
                }
            }


        // Database instance

        fun getDatabase(context: Context): PetCareDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PetCareDatabase::class.java,
                    "petcare_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7
                    )
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}