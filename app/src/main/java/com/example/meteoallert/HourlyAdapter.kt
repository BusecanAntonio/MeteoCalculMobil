package com.example.meteoallert

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HourlyAdapter(private val hourlyList: List<HourlyData>) :
    RecyclerView.Adapter<HourlyAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHour: TextView = view.findViewById(R.id.tvHour)
        val tvHourIcon: TextView = view.findViewById(R.id.tvHourIcon)
        val tvHourTemp: TextView = view.findViewById(R.id.tvHourTemp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hourly, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = hourlyList[position]
        holder.tvHour.text = item.time.substring(11)
        holder.tvHourTemp.text = "${item.temp}°C"
        
        val (_, icon) = getWeatherInfo(item.weatherCode)
        holder.tvHourIcon.text = icon
    }

    override fun getItemCount() = hourlyList.size

    private fun getWeatherInfo(code: Int): Pair<String, String> {
        return when (code) {
            0 -> "Cer senin" to "☀️"
            1, 2, 3 -> "Parțial noros" to "⛅"
            45, 48 -> "Ceață" to "🌫️"
            51, 53, 55 -> "Burniță" to "🌧️"
            61, 63, 65 -> "Ploaie" to "🌧️"
            71, 73, 75 -> "Ninsoare" to "❄️"
            77 -> "Grindină mică" to "🌨️"
            80, 81, 82 -> "Averse de ploaie" to "🌦️"
            85, 86 -> "Averse de zăpadă" to "🌨️"
            95, 96, 99 -> "Furtună" to "⛈️"
            else -> "Vreme variabilă" to "🌡️"
        }
    }
}
