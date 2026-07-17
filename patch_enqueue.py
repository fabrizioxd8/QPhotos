with open('app/src/main/java/com/example/qphotos/MainActivity.kt', 'r') as f:
    content = f.read()

old_enqueue_signature = "private fun enqueueUploadTask(photoPath: String, projectName: String, showToast: Boolean = true) {"
new_enqueue_signature = "private fun enqueueUploadTask(photoPath: String, projectName: String, showToast: Boolean = true, captureTime: String? = null, gpsCoords: String? = null, address: String? = null) {"
content = content.replace(old_enqueue_signature, new_enqueue_signature)

old_task_creation = """        val task = UploadTask(
            imagePath = photoPath,
            projectName = finalProjectName,
            status = "Pending",
            uuid = UUID.randomUUID().toString()
        )"""

new_task_creation = """        val task = UploadTask(
            imagePath = photoPath,
            projectName = finalProjectName,
            status = "Pending",
            uuid = UUID.randomUUID().toString(),
            captureTime = captureTime,
            gpsCoords = gpsCoords,
            address = address
        )"""

content = content.replace(old_task_creation, new_task_creation)

with open('app/src/main/java/com/example/qphotos/MainActivity.kt', 'w') as f:
    f.write(content)

print("Patched MainActivity.kt enqueue method")
