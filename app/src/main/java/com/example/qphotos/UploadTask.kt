package com.example.qphotos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upload_tasks")
data class UploadTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imagePath: String,
    val projectName: String,
    val uuid: String,
    var status: String = "Pending",
    val latitude: Double? = null,
    val longitude: Double? = null
)
