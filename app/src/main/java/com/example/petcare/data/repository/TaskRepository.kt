package com.example.petcare.data.repository

import com.example.petcare.data.dao.TaskDao
import com.example.petcare.data.entity.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao
) {

    suspend fun addTask(
        task: Task
    ): Long {
        return taskDao.insertTask(task)
    }

    fun getTasksForPet(
        userId: Long,
        petId: Int
    ): Flow<List<Task>> {
        return taskDao.getTasksForPet(
            userId,
            petId
        )
    }

    fun getTasksForUser(
        userId: Long
    ): Flow<List<Task>> {
        return taskDao.getTasksForUser(
            userId
        )
    }

    suspend fun getTaskById(
        taskId: Int,
        userId: Long
    ): Task? {
        return taskDao.getTaskById(
            taskId,
            userId
        )
    }

    suspend fun updateTask(
        taskId: Int,
        userId: Long,
        title: String,
        category: String,
        repeats: String,
        time: String,
        supplies: String,
        notes: String
    ) {
        taskDao.updateTask(
            taskId = taskId,
            userId = userId,
            title = title,
            category = category,
            repeats = repeats,
            time = time,
            supplies = supplies,
            notes = notes
        )
    }

    suspend fun setCompleted(
        taskId: Int,
        userId: Long,
        completed: Boolean,
        completedDate: String?
    ) {

        taskDao.setCompleted(
            taskId = taskId,
            userId = userId,
            completed = completed,
            completedDate = completedDate
        )
    }

    // =========================================================
    // DELETE SINGLE TASK
    // =========================================================

    suspend fun deleteTask(
        taskId: Int,
        userId: Long
    ) {
        taskDao.deleteTask(
            taskId,
            userId
        )
    }

    // =========================================================
    // DELETE ALL TASKS FOR A PET
    // =========================================================
    //
    // Removes every care task belonging to the specified pet.
    //

    suspend fun deleteTasksForPet(
        petId: Int,
        userId: Long
    ) {
        taskDao.deleteTasksForPet(
            petId = petId,
            userId = userId
        )
    }
}
