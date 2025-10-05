package com.example.qphotos

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaActionSound
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraHandler(private val activity: AppCompatActivity, private val onPhotoCaptured: (File) -> Unit) {

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var currentFlashMode: Int = ImageCapture.FLASH_MODE_OFF
    private val mediaActionSound = MediaActionSound()

    companion object {
        private const val TAG = "CameraHandler"
        val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun startCamera(viewFinder: PreviewView) {
        cameraExecutor = Executors.newSingleThreadExecutor()
        mediaActionSound.load(MediaActionSound.SHUTTER_CLICK)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = viewFinder.surfaceProvider
            }
            imageCapture = ImageCapture.Builder().setFlashMode(currentFlashMode).build()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(activity, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        mediaActionSound.play(MediaActionSound.SHUTTER_CLICK)

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(activity),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(activity, activity.getString(R.string.photo_capture_failed), Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onPhotoCaptured(photoFile)
                }
            }
        )
    }

    fun cycleFlashMode(btnFlash: ImageButton) {
        currentFlashMode = when (currentFlashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }
        imageCapture?.flashMode = currentFlashMode
        updateFlashButtonIcon(btnFlash)
    }

    private fun updateFlashButtonIcon(btnFlash: ImageButton) {
        when (currentFlashMode) {
            ImageCapture.FLASH_MODE_OFF -> btnFlash.setImageResource(R.drawable.ic_flash_off)
            ImageCapture.FLASH_MODE_ON -> btnFlash.setImageResource(R.drawable.ic_flash_on)
            ImageCapture.FLASH_MODE_AUTO -> btnFlash.setImageResource(R.drawable.ic_flash_auto)
        }
    }

    fun allPermissionsGranted(): Boolean = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
    }

    fun shutdown() {
        cameraExecutor.shutdown()
        mediaActionSound.release()
    }
}