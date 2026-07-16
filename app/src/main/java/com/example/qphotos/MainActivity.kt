package com.example.qphotos

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var actvNombreProyecto: android.widget.AutoCompleteTextView
    private lateinit var tvLastProject: TextView
    private lateinit var btnFlash: ImageButton
    private lateinit var tvQueueCount: TextView
    private lateinit var cameraHandler: CameraHandler

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.all { it.value }) {
            cameraHandler.startCamera(viewFinder)
        } else {
            Toast.makeText(this, getString(R.string.permissions_not_granted), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            var projectName = actvNombreProyecto.text.toString().trim()
            if (projectName.isBlank()) {
                Toast.makeText(this, getString(R.string.please_enter_project_name), Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            if (!projectName.lowercase().startsWith("c.c. ")) {
                projectName = "C.C. $projectName"
            }

            lifecycleScope.launch {
                for (uri in uris) {
                    val photoFile = copyUriToInternalStorage(uri, "GALLERY_${System.currentTimeMillis()}_${uris.indexOf(uri)}")
                    if (photoFile != null) {
                        var captureTime: String? = null
                        var gpsCoords: String? = null
                        var address: String? = null

                        try {
                            contentResolver.openInputStream(uri)?.use { inputStream ->
                                val exif = ExifInterface(inputStream)
                                captureTime = exif.getAttribute(ExifInterface.TAG_DATETIME)

                                val latLong = FloatArray(2)
                                if (exif.getLatLong(latLong)) {
                                    val lat = latLong[0].toDouble()
                                    val lon = latLong[1].toDouble()
                                    gpsCoords = "$lat, $lon"
                                    address = getAddressFromLocation(lat, lon)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        enqueueUploadTask(photoFile.absolutePath, projectName, showToast = false, captureTime, gpsCoords, address)
                    }
                }
            }
            val message = if (uris.size == 1) getString(R.string.one_photo_added_to_queue) else getString(R.string.multiple_photos_added_to_queue, uris.size)
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        actvNombreProyecto = findViewById(R.id.actvNombreProyecto)
        tvLastProject = findViewById(R.id.tvLastProject)
        tvQueueCount = findViewById(R.id.tvQueueCount)
        viewFinder = findViewById(R.id.viewFinder)
        btnFlash = findViewById(R.id.btnFlash)
        val cameraCaptureButton: ImageButton = findViewById(R.id.camera_capture_button)
        val galleryButton: ImageButton = findViewById(R.id.gallery_button)
        val settingsButton: ImageButton = findViewById(R.id.btnSettings)
        val viewProjectsButton: ImageButton = findViewById(R.id.view_projects_button)

        cameraHandler = CameraHandler(this) { photoFile ->
            val projectName = actvNombreProyecto.text.toString().trim()

            lifecycleScope.launch {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val captureTime = sdf.format(Date(photoFile.lastModified()))
                var gpsCoords: String? = null
                var address: String? = null

                val location = getCurrentLocation()
                if (location != null) {
                    gpsCoords = "${location.latitude}, ${location.longitude}"
                    address = getAddressFromLocation(location.latitude, location.longitude)
                }

                enqueueUploadTask(photoFile.absolutePath, projectName, true, captureTime, gpsCoords, address)
            }
        }

        if (cameraHandler.allPermissionsGranted()) {
            cameraHandler.startCamera(viewFinder)
        } else {
            requestPermissionLauncher.launch(CameraHandler.REQUIRED_PERMISSIONS)
        }

        cameraCaptureButton.setOnClickListener {
            val projectName = actvNombreProyecto.text.toString().trim()
            if (projectName.isBlank()) {
                Toast.makeText(this, getString(R.string.please_enter_project_name_short), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            cameraHandler.takePhoto()
        }

        btnFlash.setOnClickListener { cameraHandler.cycleFlashMode(btnFlash) }
        galleryButton.setOnClickListener { launchGallery() }
        settingsButton.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        viewProjectsButton.setOnClickListener { startActivity(Intent(this, ProjectsActivity::class.java)) }
        tvLastProject.setOnClickListener {
            actvNombreProyecto.setText(tvLastProject.text, false)
            actvNombreProyecto.requestFocus()
        }
        tvQueueCount.setOnClickListener { startActivity(Intent(this, QueueActivity::class.java)) }

        fetchLastProject()
        observeQueue()
        fetchProjectList()
    }

    private fun launchGallery() {
        val projectName = actvNombreProyecto.text.toString().trim()
        if (projectName.isBlank()) {
            Toast.makeText(this, getString(R.string.please_enter_project_name_short), Toast.LENGTH_SHORT).show()
        } else {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    private fun enqueueUploadTask(photoPath: String, projectName: String, showToast: Boolean = true, captureTime: String? = null, gpsCoords: String? = null, address: String? = null) {
        var finalProjectName = projectName
        if (!finalProjectName.lowercase().startsWith("c.c. ")) {
            finalProjectName = "C.C. $finalProjectName"
        }

        val task = UploadTask(
            imagePath = photoPath,
            projectName = finalProjectName,
            status = "Pending",
            uuid = UUID.randomUUID().toString(),
            captureTime = captureTime,
            gpsCoords = gpsCoords,
            address = address
        )

        lifecycleScope.launch {
            AppDatabase.getDatabase(applicationContext).uploadTaskDao().insert(task)
            if (showToast) {
                Toast.makeText(applicationContext, getString(R.string.photo_added_to_queue), Toast.LENGTH_SHORT).show()
            }
            scheduleUploadWorker()
        }
        tvLastProject.text = finalProjectName
        if (!tvLastProject.isVisible) { tvLastProject.isVisible = true }
    }

    private fun scheduleUploadWorker() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val uploadWorkRequest = OneTimeWorkRequest.Builder(UploadWorker::class.java)
            .setConstraints(constraints).build()
        WorkManager.getInstance(this).enqueueUniqueWork("photo_upload_work", ExistingWorkPolicy.KEEP, uploadWorkRequest)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraHandler.shutdown()
    }

    @SuppressLint("SetTextI18n")
    private fun observeQueue() {
        val dao = AppDatabase.getDatabase(this).uploadTaskDao()
        dao.getQueueAsLiveData().observe(this) { tasks ->
            val count = tasks.size
            tvQueueCount.text = getString(R.string.photos_in_queue, count)
            tvQueueCount.isVisible = count > 0
        }
    }

    private fun copyUriToInternalStorage(uri: Uri, fileName: String): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "$fileName.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun fetchLastProject() {
        ApiClient.fetchLastProject(this, object : ApiClient.ApiCallback<String> {
            override fun onSuccess(result: String) {
                runOnUiThread {
                    tvLastProject.text = result
                    tvLastProject.isVisible = result.isNotBlank()
                }
            }
            override fun onError(message: String) {
                runOnUiThread { Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
            }
        })
    }

    private fun fetchProjectList() {
        ApiClient.fetchProjectList(this, object : ApiClient.ApiCallback<List<String>> {
            override fun onSuccess(result: List<String>) {
                runOnUiThread {
                    val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, result)
                    actvNombreProyecto.setAdapter(adapter)
                }
            }
            override fun onError(message: String) {
                runOnUiThread { Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
            }
        })
    }

    private fun getCurrentLocation(): Location? {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null
            for (provider in providers) {
                val l = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            }
            return bestLocation
        }
        return null
    }

    private suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    addresses[0].getAddressLine(0)
                } else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
