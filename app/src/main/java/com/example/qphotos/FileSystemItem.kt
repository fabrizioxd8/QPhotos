package com.example.qphotos

data class FileSystemItem(
    val name: String,
    val type: String, // "folder" or "file"
    val path: String
)