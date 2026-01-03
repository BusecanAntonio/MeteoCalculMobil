package com.example.meteoallert

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.meteoallert.databinding.ActivityMainBinding
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var weatherApi: WeatherApiService
    private lateinit var sharedPrefs: SharedPreferences
    
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            getLocationAndWeather()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = getSharedPreferences("MeteoPrefs", Context.MODE_PRIVATE)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Senzori
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        weatherApi = retrofit.create(WeatherApiService::class.java)

        binding.rvForecast.layoutManager = LinearLayoutManager(this)

        binding.btnSearch.setOnClickListener {
            val query = binding.etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                searchLocation(query)
                hideKeyboard()
            }
        }

        binding.btnRefresh.setOnClickListener {
            checkPermissions()
        }

        // Pornim serviciul de alerte
        val serviceIntent = Intent(this, WeatherService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Încărcăm ultimul oraș salvat sau Baia Mare ca fallback
        val lastCity = sharedPrefs.getString("last_city", "Baia Mare") ?: "Baia Mare"
        searchLocation(lastCity)
    }

    override fun onResume() {
        super.onResume()
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    // Logica Senzor de Lumină
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val lux = event.values[0]
            if (lux < 10) { // Întuneric
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else { // Lumină
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun searchLocation(query: String) {
        lifecycleScope.launch {
            try {
                val address = getAddressFromName(query)
                if (address != null) {
                    val name = address.locality ?: query
                    binding.tvLocation.text = name
                    
                    // Salvăm orașul și coordonatele în SharedPreferences
                    sharedPrefs.edit().apply {
                        putString("last_city", name)
                        putFloat("last_lat", address.latitude.toFloat())
                        putFloat("last_lon", address.longitude.toFloat())
                        apply()
                    }
                    
                    fetchWeather(address.latitude, address.longitude)
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Eroare la căutare", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun getAddressFromName(name: String) = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
            val results = geocoder.getFromLocationName(name, 1)
            if (!results.isNullOrEmpty()) results[0] else null
        } catch (e: Exception) {
            null
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            getLocationAndWeather()
        } else {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
        
        // Notificări pentru Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocationAndWeather() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    lifecycleScope.launch {
                        val name = getCityName(location.latitude, location.longitude)
                        binding.tvLocation.text = name ?: "Locație detectată"
                        
                        name?.let {
                            sharedPrefs.edit().putString("last_city", it).apply()
                        }
                        sharedPrefs.edit().putFloat("last_lat", location.latitude.toFloat()).apply()
                        sharedPrefs.edit().putFloat("last_lon", location.longitude.toFloat()).apply()
                        
                        fetchWeather(location.latitude, location.longitude)
                    }
                }
            }
    }

    private suspend fun getCityName(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) addresses[0].locality else null
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchWeather(lat: Double, lon: Double) {
        lifecycleScope.launch {
            try {
                val response = weatherApi.getForecast(lat, lon)
                updateUI(response)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Eroare API: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(response: WeatherResponse) {
        val current = response.currentWeather
        if (current != null) {
            binding.tvCurrentTemp.text = String.format(Locale.US, "%.1f°C", current.temperature)
        }

        val daily = response.daily
        val hourly = response.hourly
        
        if (daily != null && hourly != null) {
            val list = mutableListOf<DayForecastItem>()
            for (i in daily.time.indices) {
                val startIdx = i * 24
                val endIdx = startIdx + 24
                val dayHourlyData = mutableListOf<HourlyData>()
                if (endIdx <= hourly.time.size) {
                    for (j in startIdx until endIdx) {
                        dayHourlyData.add(HourlyData(hourly.time[j], hourly.temperature2m[j], hourly.weatherCode[j]))
                    }
                }
                list.add(DayForecastItem(daily.time[i], daily.tempMin[i], daily.tempMax[i], daily.weatherCode[i], dayHourlyData))
            }
            binding.rvForecast.adapter = ForecastAdapter(list)
        }
    }
}
