with open('app/src/main/java/com/example/qphotos/MainActivity.kt', 'r') as f:
    content = f.read()

old_latlong_block = """                                val latLong = exif.latLong
                                if (latLong != null) {
                                    val lat = latLong[0]
                                    val lon = latLong[1]
                                    gpsCoords = "$lat, $lon"
                                    address = getAddressFromLocation(lat, lon)
                                }"""

new_latlong_block = """                                val latLong = FloatArray(2)
                                if (exif.getLatLong(latLong)) {
                                    val lat = latLong[0].toDouble()
                                    val lon = latLong[1].toDouble()
                                    gpsCoords = "$lat, $lon"
                                    address = getAddressFromLocation(lat, lon)
                                }"""

content = content.replace(old_latlong_block, new_latlong_block)

with open('app/src/main/java/com/example/qphotos/MainActivity.kt', 'w') as f:
    f.write(content)

print("Patched MainActivity.kt Exif latLong")
