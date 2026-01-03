package com.example.meteoallert

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true,
        @Query("daily") daily: String = "weathercode,temperature_2m_max,temperature_2m_min",
        @Query("hourly") hourly: String = "temperature_2m,weathercode",
        @Query("temperature_unit") temperatureUnit: String = "celsius",
        @Query("timezone") timezone: String = "auto"
    ): WeatherResponse
}
