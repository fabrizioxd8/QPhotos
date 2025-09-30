package com.example.qphotos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class PhotoViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_viewer)

        val viewPager: ViewPager2 = findViewById(R.id.viewPager)

        val photoUrls = intent.getStringArrayListExtra("PHOTO_URLS") ?: arrayListOf()
        val currentPosition = intent.getIntExtra("CURRENT_POSITION", 0)
        val baseUrl = intent.getStringExtra("BASE_URL") ?: ""

        val adapter = PhotoViewerAdapter(photoUrls, baseUrl)
        viewPager.adapter = adapter
        viewPager.setCurrentItem(currentPosition, false) // Go to the tapped photo
    }
}