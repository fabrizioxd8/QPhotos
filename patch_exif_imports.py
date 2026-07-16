with open('app/src/main/java/com/example/qphotos/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace('import androidx.exifinterface.media.ExifInterface', 'import android.media.ExifInterface')

with open('app/src/main/java/com/example/qphotos/MainActivity.kt', 'w') as f:
    f.write(content)

print("Patched MainActivity.kt Exif import")
