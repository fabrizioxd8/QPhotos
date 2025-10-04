package com.example.qphotos

sealed class GalleryItem {
    data class PhotoItem(val photoUrl: String) : GalleryItem()
    data class DateHeader(val date: String) : GalleryItem()
}