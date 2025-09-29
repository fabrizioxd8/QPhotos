package com.example.qphotos

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface UploadTaskDao {
    @Insert
    suspend fun insert(task: UploadTask)

    @androidx.room.Update
    suspend fun update(task: UploadTask)

    @Delete
    suspend fun delete(task: UploadTask)

    // CAMBIO: Esta función reemplaza a la antigua getPendingTasks()
    @Query("SELECT * FROM upload_tasks ORDER BY id ASC LIMIT 1")
    suspend fun getNextTask(): UploadTask? // Devuelve una sola tarea, o null si la cola está vacía

    @Query("SELECT * FROM upload_tasks ORDER BY id ASC")
    fun getQueueAsLiveData(): LiveData<List<UploadTask>>
}