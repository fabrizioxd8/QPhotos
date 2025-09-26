package com.example.qphotos

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

class GalleryActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    // NUEVO: El adapter ahora es una variable de la clase
    private lateinit var galleryAdapter: GalleryAdapter
    private val TAG = "GalleryDebug"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        val projectName = intent.getStringExtra("PROJECT_NAME")
        val monthFolder = intent.getStringExtra("MONTH_FOLDER")

        Log.d(TAG, "Activity started with project: $projectName, month: $monthFolder")

        if (projectName == null || monthFolder == null) {
            Toast.makeText(this, "Error: No se encontró el proyecto", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        title = projectName

        // CAMBIO: Configuramos el RecyclerView y el adapter UNA SOLA VEZ aquí
        setupRecyclerView()

        fetchPhotos(monthFolder, projectName)
    }

    // CAMBIO: Esta función ahora solo configura la estructura, no los datos
    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.galleryRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3) // 3 columnas
        galleryAdapter = GalleryAdapter() // Creamos el adapter vacío
        recyclerView.adapter = galleryAdapter
    }

    private fun fetchPhotos(month: String, project: String) {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null) ?: return
        val baseUrl = "http://$ip:5000"
        val url = "$baseUrl/photos/$month/$project"

        Log.d(TAG, "Fetching photos from URL: $url")

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to fetch photos: ${e.message}")
                runOnUiThread { Toast.makeText(applicationContext, "Fallo al obtener fotos", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                // --- THE FIX IS HERE ---
                // We use '.let' to safely handle the response body
                response.body.let { responseBody ->
                    if (response.isSuccessful) {
                        val responseBodyString = responseBody.string()
                        Log.d(TAG, "Received JSON response: $responseBodyString")

                        val photoUrls = mutableListOf<String>()
                        try {
                            val jsonArray = JSONArray(responseBodyString)
                            for (i in 0 until jsonArray.length()) {
                                photoUrls.add(jsonArray.getString(i))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing JSON", e)
                        }

                        Log.d(TAG, "Parsed ${photoUrls.size} photo URLs.")

                        runOnUiThread {
                            galleryAdapter.updatePhotos(photoUrls, baseUrl)
                        }
                    } else {
                        Log.e(TAG, "Server returned error: ${response.code}")
                    }
                }
                // --- END OF FIX ---
            }
        })
    }
    }