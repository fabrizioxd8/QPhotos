with open('app/src/main/java/com/example/qphotos/MainActivity.kt', 'r') as f:
    content = f.read()

old_callback = """        cameraHandler = CameraHandler(this) { photoFile ->
            val projectName = actvNombreProyecto.text.toString().trim()
            enqueueUploadTask(photoFile.absolutePath, projectName)
        }"""

new_callback = """        cameraHandler = CameraHandler(this) { photoFile ->
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
        }"""

content = content.replace(old_callback, new_callback)

with open('app/src/main/java/com/example/qphotos/MainActivity.kt', 'w') as f:
    f.write(content)

print("Patched MainActivity.kt camera callback")
