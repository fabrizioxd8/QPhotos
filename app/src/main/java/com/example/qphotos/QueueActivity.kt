package com.example.qphotos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class QueueActivity : AppCompatActivity() {

    private lateinit var queueAdapter: QueueAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_queue)

        // Set up the RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.queueRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        queueAdapter = QueueAdapter(emptyList())
        recyclerView.adapter = queueAdapter

        // Get the database and start observing the queue
        val dao = AppDatabase.getDatabase(this).uploadTaskDao()
        dao.getQueueAsLiveData().observe(this) { tasks ->
            // This will automatically run every time the data changes
            tasks?.let {
                queueAdapter.updateTasks(it)
            }
        }
    }
}