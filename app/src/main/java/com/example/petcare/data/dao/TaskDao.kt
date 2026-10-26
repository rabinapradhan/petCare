package com.example.petcare.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.petcare.data.entity.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert
    suspend fun insertTask(task: Task): Long

    @Query("""
        SELECT * FROM tasks
        WHERE userId = :userId
        AND petId = :petId
        ORDER BY id DESC
    """)
    fun getTasksForPet(
        userId: Long,
        petId: Int
    ): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks
        WHERE userId = :userId
        ORDER BY id DESC
    """)
    fun getTasksForUser(
        userId: Long
    ): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks
        WHERE id = :taskId
        AND userId = :userId
        LIMIT 1
    """)
    suspend fun getTaskById(
        taskId: Int,
        userId: Long
    ): Task?

    @Query("""
        UPDATE tasks
        SET title = :title,
            category = :category,
            repeats = :repeats,
            time = :time,
            supplies = :supplies,
            notes = :notes
        WHERE id = :taskId
        AND userId = :userId
    """)
    suspend fun updateTask(
        taskId: Int,
        userId: Long,
        title: String,
        category: String,
        repeats: String,
        time: String,
        supplies: String,
        notes: String
    )

    @Query("""
        UPDATE tasks
        SET completed = :completed,
            completedDate = :completedDate
        WHERE id = :taskId
        AND userId = :userId
    """)
    suspend fun setCompleted(
        taskId: Int,
        userId: Long,
        completed: Boolean,
        completedDate: String?
    )


    // DELETE SINGLE TASK

    @Query("""
        DELETE FROM tasks
        WHERE id = :taskId
        AND userId = :userId
    """)
    suspend fun deleteTask(
        taskId: Int,
        userId: Long
    )


    // DELETE ALL TASKS FOR A PET

    // This is used before deleting a pet.
    // It prevents orphaned tasks from remaining in the
    // database after their associated pet has been deleted.


    @Query("""
        DELETE FROM tasks
        WHERE petId = :petId
        AND userId = :userId
    """)
    suspend fun deleteTasksForPet(
        petId: Int,
        userId: Long
    )
}
