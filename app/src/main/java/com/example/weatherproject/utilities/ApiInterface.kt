package com.example.weatherproject.utilities

import com.example.weatherproject.models.AirModel
import com.example.weatherproject.models.ForecastModel
import com.example.weatherproject.models.WeatherModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {

    @GET("weather?")
    fun getCurrentWeatherData(
        @Query("lat") lat:String,
        @Query("lon") lon:String,
        @Query("appid") appid:String,
        @Query("lang") lang:String,
        @Query("units") unit:String
    ): Call<WeatherModel>

    @GET("weather?")
    fun getCityWeatherData(
        @Query("q") q:String,
        @Query("appid") appid: String,
        @Query("lang") lang:String,
        @Query("units") unit:String
    ):Call<WeatherModel>

    //XD
    @GET("air_pollution?")
    fun getCurrentAirPollution(
        @Query("lat") lat:String,
        @Query("lon") lon:String,
        @Query("appid") appid:String
    ): Call<AirModel>

    @GET("forecast?")
    fun getForecastWeatherData(
        @Query("lat") lat:String,
        @Query("lon") lon:String,
        @Query("appid") appid:String,
        @Query("lang") lang:String,
        @Query("units") unit:String
    ): Call<ForecastModel>
}