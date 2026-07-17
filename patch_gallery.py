with open('app/src/main/java/com/example/qphotos/MainActivity.kt', 'r') as f:
    content = f.read()

old_gallery_block = """            for (uri in uris) {
                val photoFile = copyUriToInternalStorage(uri, "GALLERY_${System.currentTimeMillis()}_${uris.indexOf(uri)}")
                if (photoFile != null) {
                    enqueueUploadTask(photoFile.absolutePath, projectName, showToast = false)
                }
            }"""

new_gallery_block = """            lifecycleScope.launch {
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

                                val latLong = exif.latLong
                                if (latLong != null) {
                                    val lat = latLong[0]
                                    val lon = latLong[1]
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
            }"""

content = content.replace(old_gallery_block, new_gallery_block)

with open('app/src/main/java/com/example/qphotos/MainActivity.kt', 'w') as f:
    f.write(content)

print("Patched MainActivity.kt gallery logic")
