with open('app/src/main/java/com/example/qphotos/UploadTask.kt', 'r') as f:
    content = f.read()

new_content = content.replace(
    'val uuid: String,\n    var status: String = "Pending"',
    'val uuid: String,\n    val captureTime: String? = null,\n    val gpsCoords: String? = null,\n    val address: String? = null,\n    var status: String = "Pending"'
)

with open('app/src/main/java/com/example/qphotos/UploadTask.kt', 'w') as f:
    f.write(new_content)

with open('app/src/main/java/com/example/qphotos/AppDatabase.kt', 'r') as f:
    content = f.read()

new_content = content.replace(
    'version = 2',
    'version = 3'
)

with open('app/src/main/java/com/example/qphotos/AppDatabase.kt', 'w') as f:
    f.write(new_content)

print("Patched UploadTask.kt and AppDatabase.kt")
