package com.example.qphotos

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var etServerIp: EditText
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize SharedPreferences
        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        etServerIp = findViewById(R.id.etServerIp)
        val btnSaveIp: Button = findViewById(R.id.btnSaveIp)

        // Load and display the saved IP when the screen opens
        loadIpAddress()

        btnSaveIp.setOnClickListener {
            saveIpAddress()
        }
    }

    private fun loadIpAddress() {
        // We get the saved IP. If none is saved, the default is an empty string.
        val savedIp = prefs.getString("server_ip", "")
        etServerIp.setText(savedIp)
    }

    private fun saveIpAddress() {
        val newIp = etServerIp.text.toString().trim()
        if (newIp.isNotEmpty()) {
            // Save the new IP to SharedPreferences
            prefs.edit().putString("server_ip", newIp).apply()
            Toast.makeText(this, "Dirección IP guardada", Toast.LENGTH_SHORT).show()
            finish() // Close the settings screen and return to the main screen
        } else {
            Toast.makeText(this, "La dirección IP no puede estar vacía", Toast.LENGTH_SHORT).show()
        }
    }
}