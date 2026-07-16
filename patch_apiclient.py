with open('app/src/main/java/com/example/qphotos/ApiClient.kt', 'r') as f:
    content = f.read()

# Find the uploadPhoto function
old_build_block = """        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
            .addFormDataPart("project_name", task.projectName)
            .addFormDataPart("uuid", task.uuid)
            .build()"""

new_build_block = """        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
            .addFormDataPart("project_name", task.projectName)
            .addFormDataPart("uuid", task.uuid)

        task.captureTime?.let { builder.addFormDataPart("capture_time", it) }
        task.gpsCoords?.let { builder.addFormDataPart("gps_coords", it) }
        task.address?.let { builder.addFormDataPart("address", it) }

        val requestBody = builder.build()"""

content = content.replace(old_build_block, new_build_block)

with open('app/src/main/java/com/example/qphotos/ApiClient.kt', 'w') as f:
    f.write(content)

print("Patched ApiClient.kt")
