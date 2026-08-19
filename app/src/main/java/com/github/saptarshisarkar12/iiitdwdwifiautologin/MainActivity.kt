package com.github.saptarshisarkar12.iiitdwdwifiautologin

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPermissions()
        setupNetworkListener()

        val sharedPreferences = getSharedPreferences("secure_login_prefs", MODE_PRIVATE)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnSave = findViewById<Button>(R.id.btnSave)

        val savedUser = sharedPreferences.getString("username", "") ?: ""
        val savedPass = sharedPreferences.getString("password", "") ?: ""

        if (savedUser.isNotEmpty()) etUsername.setText(KeystoreHelper.decrypt(savedUser))
        if (savedPass.isNotEmpty()) etPassword.setText(KeystoreHelper.decrypt(savedPass))

        btnSave.setOnClickListener {
            val user = etUsername.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (user.isNotEmpty() && pass.isNotEmpty()) {
                sharedPreferences.edit {
                    putString("username", KeystoreHelper.encrypt(user))
                    putString("password", KeystoreHelper.encrypt(pass))
                }
                Toast.makeText(this, "Credentials securely saved", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val fineLoc =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineLoc != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else {
            val bgLoc = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
            if (bgLoc != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    2
                )
            }
        }
    }

    private fun setupNetworkListener() {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        // clearCapabilities() ensures the callback fires for captive portals
        val request = NetworkRequest.Builder()
            .clearCapabilities()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        val intent = Intent(this, WifiReceiver::class.java).apply {
            action = "com.github.saptarshisarkar12.iiitdwdwifiautologin.WIFI_TRIGGER"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        cm.registerNetworkCallback(request, pendingIntent)
    }
}
