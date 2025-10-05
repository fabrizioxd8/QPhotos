package com.example.qphotos

data class FileSystemItem(
    val name: String,
    val path: String,
    val type: String // "month", "project", or "day"
)
