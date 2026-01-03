package com.example.meteoallert

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var weatherApi: WeatherApiService
    private val CHANNEL_ID = "WeatherAlertChannel"

    override fun onCreate() {
        super.onCreate()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        weatherApi = retrofit.create(WeatherApiService::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("Monitorizare meteo activă")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, notification)
        }

        serviceScope.launch {
            while (isActive) {
                checkWeatherAlerts()
                delay(3 * 60 * 60 * 1000) // 3 ore
            }
        }

        return START_STICKY
    }

    private suspend fun checkWeatherAlerts() {
        val prefs = getSharedPreferences("MeteoPrefs", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("last_lat", 47.66f).toDouble()
        val lon = prefs.getFloat("last_lon", 23.57f).toDouble()

        try {
            val response = weatherApi.getForecast(lat, lon)
            val temp = response.currentWeather?.temperature ?: 0.0
            
            if (temp > 35 || temp < -10) {
                sendAlertNotification("Alertă temperatură extremă: $temp°C!")
            }
            
            // Verificare furtună (coduri WMO 95, 96, 99)
            if (response.currentWeather?.weatherCode ?: 0 >= 95) {
                sendAlertNotification("Alertă furtună în zona ta!")
            }
        } catch (e: Exception) {
            Log.e("WeatherService", "Eroare la verificarea alertelor: ${e.message}")
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Meteo Allert")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun sendAlertNotification(message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val alertNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⚠️ Alertă Meteo!")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(2, alertNotification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alerte Meteo",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
