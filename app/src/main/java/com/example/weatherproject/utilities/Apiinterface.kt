package com.example.weatherproject.utilities

import com.example.weatherproject.models.weathermodel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface Apiinterface {

    @GET("weather?")
    fun getCurrentWeatherData(
        @Query("lat") lat:String,
        @Query("lon") lon:String,
        @Query("appid") appid:String
    ): Call<weathermodel>

    @GET("weather?")
    fun getCityWeatherData(
        @Query("q") q:String,
        @Query("appid") appid: String
    ):Call<weathermodel>
}