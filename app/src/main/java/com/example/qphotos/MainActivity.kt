package com.example.qphotos

import android.os.Build
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import java.util.UUID

class MainActivity : AppCompatActivity() {

    // --- (Most of your variables are the same) ---
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewFinder: PreviewView
    private lateinit var etNombreProyecto: EditText
    private lateinit var tvLastProject: TextView
    private lateinit var btnFlash: ImageButton
    private var currentFlashMode: Int = ImageCapture.FLASH_MODE_OFF
    private lateinit var tvQueueCount: TextView
    private val client = OkHttpClient()

    // The gallery launcher is updated to call the new scheduling function
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            var projectName = etNombreProyecto.text.toString().trim()
            if (projectName.isBlank()) {
                Toast.makeText(this, "Por favor, escribe un nombre de proyecto primero", Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            if (!projectName.lowercase().startsWith("c.c. ")) {
                projectName = "C.C. " + projectName
            }
            for (uri in uris) {
                val photoFile = copyUriToInternalStorage(uri, "GALLERY_${System.currentTimeMillis()}_${uris.indexOf(uri)}")
                if (photoFile != null) {
                    enqueueUploadTask(photoFile.absolutePath, projectName, showToast = false)
                }
            }
            val message = if (uris.size == 1) "1 foto añadida a la cola." else "${uris.size} fotos añadidas a la cola."
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- (Connecting UI is the same) ---
        etNombreProyecto = findViewById(R.id.etNombreProyecto)
        tvLastProject = findViewById(R.id.tvLastProject)
        tvQueueCount = findViewById(R.id.tvQueueCount)
        viewFinder = findViewById(R.id.viewFinder)
        val cameraCaptureButton: ImageButton = findViewById(R.id.camera_capture_button)
        val galleryButton: ImageButton = findViewById(R.id.gallery_button) // Using the correct ID
        val settingsButton: ImageButton = findViewById(R.id.btnSettings)
        val viewProjectsButton: ImageButton = findViewById(R.id.view_projects_button) // Using the correct ID
        btnFlash = findViewById(R.id.btnFlash)

        // --- (Permission logic is the same) ---
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // --- (Button listeners are the same, but uploadGalleryButton is fixed) ---
        cameraCaptureButton.setOnClickListener { takePhoto() }
        galleryButton.setOnClickListener {
            val projectName = etNombreProyecto.text.toString().trim()
            if(projectName.isBlank()) {
                Toast.makeText(this, "Por favor, escribe un nombre de proyecto", Toast.LENGTH_SHORT).show()
            } else {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
        settingsButton.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        viewProjectsButton.setOnClickListener { startActivity(Intent(this, ProjectsActivity::class.java)) }
        tvLastProject.setOnClickListener { etNombreProyecto.setText(tvLastProject.text) }
        tvQueueCount.setOnClickListener { startActivity(Intent(this, QueueActivity::class.java)) }

        btnFlash.setOnClickListener {
            currentFlashMode = when (currentFlashMode) {
                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                else -> ImageCapture.FLASH_MODE_OFF
            }
            updateFlashButtonIcon()
            imageCapture?.flashMode = currentFlashMode
        }

        // --- (The rest of onCreate is the same) ---
        cameraExecutor = Executors.newSingleThreadExecutor()
        fetchLastProject()
        observeQueue()

        // CAMBIO: The call to processUploadQueue() is removed from here.
        // WorkManager will handle starting the queue automatically.
    }

    // --- CAMBIO: The 'takePhoto' function now calls 'enqueueUploadTask' ---
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        var projectName = etNombreProyecto.text.toString().trim()
        if (projectName.isBlank()) {
            Toast.makeText(this, "Por favor, escribe un nombre de proyecto", Toast.LENGTH_SHORT).show()
            return
        }
        if (!projectName.lowercase().startsWith("c.c. ")) {
            projectName = "C.C. " + projectName
        }

        val photoFile = File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Fallo al tomar la foto: ${exc.message}", exc)
                    Toast.makeText(baseContext, "Fallo al guardar foto", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    enqueueUploadTask(photoFile.absolutePath, projectName)
                }
            }
        )
    }

    // --- CAMBIO: This function now just adds to the DB and schedules the work ---
    private fun enqueueUploadTask(photoPath: String, projectName: String, showToast: Boolean = true) {
        val task = UploadTask(
            imagePath = photoPath,
            projectName = projectName,
            uuid = UUID.randomUUID().toString() // <-- THE FIX IS HERE
        )

        lifecycleScope.launch {
            AppDatabase.getDatabase(applicationContext).uploadTaskDao().insert(task)
            if (showToast) {
                Toast.makeText(applicationContext, "Foto añadida a la cola.", Toast.LENGTH_SHORT).show()
            }
            scheduleUploadWorker()
        }
        tvLastProject.text = projectName
        if (!tvLastProject.isVisible) { tvLastProject.isVisible = true }
    }

    private fun updateFlashButtonIcon() {
        when (currentFlashMode) {
            ImageCapture.FLASH_MODE_OFF -> btnFlash.setImageResource(R.drawable.ic_flash_off)
            ImageCapture.FLASH_MODE_ON -> btnFlash.setImageResource(R.drawable.ic_flash_on)
            ImageCapture.FLASH_MODE_AUTO -> btnFlash.setImageResource(R.drawable.ic_flash_auto)
        }
    }

    // --- NUEVO: This is the function that schedules the background job ---
    private fun scheduleUploadWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // CAMBIO: Usamos el método clásico con 'Builder' que es más robusto
        val uploadWorkRequest = OneTimeWorkRequest.Builder(UploadWorker::class.java)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "photo_upload_work",
            ExistingWorkPolicy.KEEP,
            uploadWorkRequest
        )
        Log.d(TAG, "Unique UploadWorker job has been enqueued.")
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = viewFinder.surfaceProvider
            }

            imageCapture = ImageCapture.Builder()
                .setFlashMode(currentFlashMode)
                .build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Fallo al vincular los casos de uso", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permisos no concedidos por el usuario.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    // NUEVO: Movimos los permisos y constantes de cámara aquí
    companion object {
        private const val TAG = "CameraXApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            } else {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
        private const val LAST_PROJECT_PATH = "/last-project"
    }



    // NUEVO: La lógica para observar la cola ahora está en su propia función
    @SuppressLint("SetTextI18n")
    private fun observeQueue() {
        val dao = AppDatabase.getDatabase(this).uploadTaskDao()
        dao.getQueueAsLiveData().observe(this) { tasks ->
            val count = tasks.size
            if (count > 0) {
                tvQueueCount.text = "$count foto(s) en cola"
                tvQueueCount.isVisible = true
            } else {
                tvQueueCount.isVisible = false
            }
        }
    }

private fun copyUriToInternalStorage(uri: Uri, fileName: String): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "$fileName.jpg")
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
    private fun getBaseUrl(): String? {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null)
        return if (ip.isNullOrBlank()) {
            Toast.makeText(this, "Por favor, configura la IP del servidor en los ajustes.", Toast.LENGTH_LONG).show()
            null
        } else {
            "http://$ip:5000"
        }
    }
    private fun fetchLastProject() {
        val baseUrl = getBaseUrl() ?: return // Get the saved IP or stop if it's not set
        val request = Request.Builder().url(baseUrl + LAST_PROJECT_PATH).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread { Toast.makeText(applicationContext, "No se pudo obtener el último proyecto", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBodyString = response.body.string()
                    // Add a null check here
                    val json = JSONObject(responseBodyString)
                    val lastProject = json.optString("last_project", "")
                    runOnUiThread {
                        if (lastProject.isNotBlank()) {
                            tvLastProject.text = lastProject
                            tvLastProject.isVisible = true
                        } else {
                            tvLastProject.isVisible = false
                        }
                    }
                }
            }
        })
    }

}