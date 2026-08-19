package com.github.saptarshisarkar12.iiitdwdwifiautologin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class WifiReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // goAsync() prevents Android from killing the background process
        val pendingResult = goAsync()

        Thread {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

                // CRITICAL FIX: We must use the deprecated connectionInfo here to bypass Android 12+ SSID redaction
                @Suppress("DEPRECATION")
                val wifiInfo = wifiManager.connectionInfo
                val ssid = wifiInfo?.ssid?.replace("\"", "")

                if (ssid == "IIIT DHARWAD-STUDENT") {
                    performAutoLogin(context)
                }
            } finally {
                pendingResult.finish() // Tells the OS we are done
            }
        }.start()
    }

    private fun performAutoLogin(context: Context) {
        try {
            val prefs = context.getSharedPreferences("secure_login_prefs", Context.MODE_PRIVATE)
            val encUser = prefs.getString("username", "") ?: ""
            val encPass = prefs.getString("password", "") ?: ""

            if (encUser.isEmpty() || encPass.isEmpty()) return

            val username = KeystoreHelper.decrypt(encUser)
            val password = KeystoreHelper.decrypt(encPass)

            val url = URL("http://172.16.16.16:8090/login.xml")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.doOutput = true

            val encUserUrl = URLEncoder.encode(username, "UTF-8")
            val encPassUrl = URLEncoder.encode(password, "UTF-8")
            val payload = "mode=191&username=$encUserUrl&password=$encPassUrl&a=1786430697920&producttype=0"

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload)
                writer.flush()
            }

            conn.responseCode
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
