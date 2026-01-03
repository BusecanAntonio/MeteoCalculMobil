package com.example.meteoallert

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.meteoallert.databinding.ActivityDetailBinding
import kotlin.math.sqrt

class DetailActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Preluare date prin Intent
        val date = intent.getStringExtra("date") ?: ""
        val temps = intent.getDoubleArrayExtra("temps") ?: doubleArrayOf()
        val times = intent.getStringArrayExtra("times") ?: arrayOf()
        val codes = intent.getIntArrayExtra("codes") ?: intArrayOf()

        binding.tvDetailDate.text = "Prognoză: $date"

        // Pregătire date pentru RecyclerView
        val hourlyList = mutableListOf<HourlyData>()
        for (i in temps.indices) {
            hourlyList.add(HourlyData(times[i], temps[i], codes[i]))
        }

        binding.rvHourlyDetail.layoutManager = LinearLayoutManager(this)
        binding.rvHourlyDetail.adapter = HourlyAdapter(hourlyList)

        // Senzor pentru "Shake to go back"
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        acceleration = 10f
        currentAcceleration = SensorManager.GRAVITY_EARTH
        lastAcceleration = SensorManager.GRAVITY_EARTH
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            
            lastAcceleration = currentAcceleration
            currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta

            if (acceleration > 12) {
                finish() // Închide activitatea la scuturare
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}
