package com.example.weatherproject.models

data class AirModel(
    val coord: Coord,
    val list: List<AirInfo>,
)

data class AirInfo(
    val dt: Int,
    val main: AirMain,
    val components: Components
)

data class AirMain(
    val aqi:Int
)

data class Components(
    val co: Double,
    val no: Double,
    val no2: Double,
    val o3: Double,
    val so2: Double,
    val pm2_5: Double,
    val pm10: Double,
    val nh3: Double
)