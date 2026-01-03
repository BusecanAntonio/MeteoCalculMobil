package com.example.meteoallert

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class HourlyData(
    val time: String,
    val temp: Double,
    val weatherCode: Int
)

data class DayForecastItem(
    val date: String,
    val minTemp: Double,
    val maxTemp: Double,
    val weatherCode: Int,
    val hourlyData: List<HourlyData>
)

class ForecastAdapter(private val items: List<DayForecastItem>) :
    RecyclerView.Adapter<ForecastAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.tvDate)
        val weatherDesc: TextView = view.findViewById(R.id.tvWeatherDesc)
        val weatherIcon: TextView = view.findViewById(R.id.tvWeatherIcon)
        val tempText: TextView = view.findViewById(R.id.tvDayTemp)
        val header: LinearLayout = view.findViewById(R.id.headerLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_daily, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.dateText.text = item.date
        holder.tempText.text = "${item.minTemp}°C ... ${item.maxTemp}°C"
        
        val (desc, icon) = getWeatherInfo(item.weatherCode)
        holder.weatherDesc.text = desc
        holder.weatherIcon.text = icon

        holder.header.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra("date", item.date)
                putExtra("temps", item.hourlyData.map { it.temp }.toDoubleArray())
                putExtra("times", item.hourlyData.map { it.time }.toTypedArray())
                putExtra("codes", item.hourlyData.map { it.weatherCode }.toIntArray())
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = items.size

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
