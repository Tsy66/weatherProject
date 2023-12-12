package com.example.weatherproject.utilities

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Apiutilities {

    private var retrofit: Retrofit?=null

    private var BASE_URL="https://api.openweathermap.org/data/2.5/"

    fun getApiInterface():Apiinterface?{

        if (retrofit==null){

            retrofit= Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build()
        }

        return retrofit?.create(Apiinterface::class.java)
    }
}