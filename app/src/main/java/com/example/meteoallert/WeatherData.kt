package com.example.meteoallert

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    @SerializedName("current_weather")
    val currentWeather: CurrentWeather?,
    val daily: DailyForecast?,
    val hourly: HourlyForecast?
)

data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    @SerializedName("weathercode")
    val weatherCode: Int,
    val time: String
)

data class DailyForecast(
    val time: List<String>,
    @SerializedName("weathercode")
    val weatherCode: List<Int>,
    @SerializedName("temperature_2m_max")
    val tempMax: List<Double>,
    @SerializedName("temperature_2m_min")
    val tempMin: List<Double>
)

data class HourlyForecast(
    val time: List<String>,
    @SerializedName("temperature_2m")
    val temperature2m: List<Double>,
    @SerializedName("weathercode")
    val weatherCode: List<Int>
)
