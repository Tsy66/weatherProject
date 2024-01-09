package com.example.weatherproject.models

data class ForecastModel(
    val cod: Int,
    val message: String,
    val cnt: Int,
    val list: List<WeatherList>,
    val city: City
)

data class WeatherList(
    val dt: Long,
    val main: ForecastMain,
    val weather: List<Weather>,
    val clouds: Clouds,
    val wind: Wind,
    val visibility: Int,
    val pop: Double, //降雨機率 0.0~1.0
    val rain: ForecastRain,
    //val snow: Snow,
    val sys: Sys,
    val dt_txt: String
)

data class City(
    val id: Int,
    val name: String,
    val coord: Coord,
    val country: String,
    val population: Int,
    val timezone: Int,
    val sunrise: Long,
    val sunset: Long
)

data class ForecastMain(
    val feels_like: Double,
    val grnd_level: Int,
    val humidity: Int,
    val pressure: Int,
    val sea_level: Int,
    val temp: Double,
    val temp_max: Double,
    val temp_min: Double,
    val temp_kf: Double
)

data class ForecastRain(
    val `3h`: Double //過去3小時
)
