package com.example.qphotos // Change to your package name

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upload_tasks")
data class UploadTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imagePath: String,
    val projectName: String,
    val uuid: String,
    val captureTime: String? = null,
    val gpsCoords: String? = null,
    val address: String? = null,
    var status: String = "Pending"
)