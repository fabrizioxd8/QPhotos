with open('app/src/main/AndroidManifest.xml', 'r') as f:
    data = f.read()
if '<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"' not in data:
    print("Patching failed")
else:
    print("Patching succeeded")
