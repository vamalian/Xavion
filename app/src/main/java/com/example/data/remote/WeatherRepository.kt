package com.example.data.remote

import com.example.data.model.WeatherInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class WeatherRepository(private val httpClient: OkHttpClient) {

    suspend fun fetchWeatherForCity(city: String): Result<WeatherInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanCity = city.trim().ifBlank { "San Francisco" }
            val encoded = URLEncoder.encode(cleanCity, StandardCharsets.UTF_8.name())
            val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json"
            val geoReq = Request.Builder().url(geoUrl).get().build()
            val geoJson = httpClient.newCall(geoReq).execute().use { resp ->
                if (!resp.isSuccessful) error("Weather geocoding HTTP ${resp.code}")
                JSONObject(resp.body?.string().orEmpty())
            }
            val results = geoJson.optJSONArray("results")
                ?: error("City '$cleanCity' not found.")
            if (results.length() == 0) error("City '$cleanCity' not found.")
            val first = results.getJSONObject(0)
            val lat = first.getDouble("latitude")
            val lon = first.getDouble("longitude")
            val resolvedName = first.optString("name", cleanCity)

            val weatherUrl =
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code,wind_speed_10m"
            val wReq = Request.Builder().url(weatherUrl).get().build()
            val wJson = httpClient.newCall(wReq).execute().use { resp ->
                if (!resp.isSuccessful) error("Weather forecast HTTP ${resp.code}")
                JSONObject(resp.body?.string().orEmpty())
            }
            val current = wJson.getJSONObject("current")
            val temp = current.getDouble("temperature_2m")
            val code = current.optInt("weather_code", 0)
            val wind = current.optDouble("wind_speed_10m", 0.0)

            WeatherInfo(
                city = resolvedName,
                temperatureCelsius = temp,
                conditionLabel = mapWmoCode(code),
                windKmh = wind,
                updatedAtMillis = System.currentTimeMillis()
            )
        }
    }

    private fun mapWmoCode(code: Int): String = when (code) {
        0 -> "Clear Sky"
        1, 2 -> "Partly Cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rain"
        71, 73, 75 -> "Snow"
        80, 81, 82 -> "Rain Showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Atmospheric"
    }
}
