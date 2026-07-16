with open('app/src/main/java/com/example/qphotos/MainActivity.kt', 'r') as f:
    content = f.read()

# Add new methods at the end of the class
methods_to_add = """

    private fun getCurrentLocation(): Location? {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null
            for (provider in providers) {
                val l = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            }
            return bestLocation
        }
        return null
    }

    private suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    addresses[0].getAddressLine(0)
                } else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
"""

content = content.replace('\n}\n', methods_to_add)

with open('app/src/main/java/com/example/qphotos/MainActivity.kt', 'w') as f:
    f.write(content)

print("Patched MainActivity.kt with new methods")
