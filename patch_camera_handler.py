import re

with open('app/src/main/java/com/example/qphotos/CameraHandler.kt', 'r') as f:
    content = f.read()

# Replace REQUIRED_PERMISSIONS array
old_permissions_tiramisu = "arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.POST_NOTIFICATIONS)"
new_permissions_tiramisu = "arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)"
content = content.replace(old_permissions_tiramisu, new_permissions_tiramisu)

old_permissions_legacy = "arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)"
new_permissions_legacy = "arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)"
content = content.replace(old_permissions_legacy, new_permissions_legacy)

with open('app/src/main/java/com/example/qphotos/CameraHandler.kt', 'w') as f:
    f.write(content)

print("Patched CameraHandler.kt")
