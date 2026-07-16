with open('app/src/main/java/com/example/qphotos/MainActivity.kt', 'r') as f:
    content = f.read()

imports_to_add = """
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
"""

# Insert imports after existing imports
content = content.replace('import java.util.UUID\n', 'import java.util.UUID\n' + imports_to_add)

with open('app/src/main/java/com/example/qphotos/MainActivity.kt', 'w') as f:
    f.write(content)

print("Patched MainActivity.kt imports")
