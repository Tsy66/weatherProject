package com.example.weatherproject.activities

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.example.weatherproject.R
import com.example.weatherproject.databinding.ActivityMainBinding
import com.example.weatherproject.models.weathermodel
import com.example.weatherproject.utilities.Apiutilities
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var currentLocation: Location
    private lateinit var fuseLocationProvider: FusedLocationProviderClient
    private val LOCATION_REQUEST_CODE=101
    private val apiKey="3aaabb52ca2aefd0c8266124993e8183"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        fuseLocationProvider = LocationServices.getFusedLocationProviderClient(this)

        getCurrentLocation()

        binding.locationSearch.setOnEditorActionListener { textView, i, keyEvent ->

            if (i == EditorInfo.IME_ACTION_SEARCH) {

                getCityWeather(binding.locationSearch.text.toString())

                binding.locationSearch.text.clear()

                val view = this.currentFocus

                if (view!=null){

                    val imm:InputMethodManager=getSystemService(INPUT_METHOD_SERVICE)
                    as InputMethodManager

                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    binding.locationSearch.clearFocus()
                }

                return@setOnEditorActionListener true

            }
            else{

                return@setOnEditorActionListener false
            }

        }

        binding.currentLocation.setOnClickListener {

            getCurrentLocation()
        }

        binding.searchOption.setOnClickListener {

            binding.locationOptionBar.visibility = View.GONE
            binding.locationSearchBar.visibility = View.VISIBLE
        }

        binding.back.setOnClickListener {

            hideKeyboard(this)

            binding.locationOptionBar.visibility = View.VISIBLE
            binding.locationSearchBar.visibility = View.GONE

        }


    }

    private fun hideKeyboard(activity: Activity){

        val imm = activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        var view = activity.currentFocus
        if (view == null) {

            view = View(activity)

        }

        imm.hideSoftInputFromWindow(view.windowToken, 0)

    }

    private fun getCurrentLocation() {
        if (checkPermissions()) {
            if (isLocationEnaled()) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions()
                    return
                }

                fuseLocationProvider.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            currentLocation = location
                            binding.progressBar.visibility = View.VISIBLE

                            Log.d("Location", "Latitude: ${location.latitude}, Longitude: ${location.longitude}")

                            fetchCurrentLocationWeather(
                                location.latitude.toString(),
                                location.longitude.toString()
                            )
                        } else {
                            Log.e("Location", "Unable to retrieve current location.")
                        }
                    }
            } else {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }


    private fun getCityWeather(city:String){
        binding.progressBar.visibility = View.VISIBLE

        Apiutilities.getApiInterface()?.getCityWeatherData(city, apiKey)
            ?.enqueue(object : Callback<weathermodel>{

                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<weathermodel>,
                    response: Response<weathermodel>
                ) {

                    if (response.isSuccessful){

                        binding.locationOptionBar.visibility = View.VISIBLE
                        binding.locationSearchBar.visibility = View.GONE
                        binding.progressBar.visibility = View.GONE

                        response.body()?.let{
                            setData(it)
                        }
                    }
                    else{

                        binding.locationOptionBar.visibility = View.VISIBLE
                        binding.locationSearchBar.visibility = View.GONE
                        binding.progressBar.visibility = View.GONE

                        Toast.makeText(this@MainActivity, "No City Found", Toast.LENGTH_SHORT).show()

                    }
                }

                override fun onFailure(call: Call<weathermodel>, t: Throwable) {

                }


            })
    }

    private fun fetchCurrentLocationWeather(latitude: String, longitude: String) {
        Apiutilities.getApiInterface()?.getCurrentWeatherData(latitude, longitude, apiKey)
            ?.enqueue(object : Callback<weathermodel> {

                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<weathermodel>,
                    response: Response<weathermodel>
                ) {

                    if (response.isSuccessful) {
                        binding.progressBar.visibility = View.GONE

                        response.body()?.let {
                            setData(it)
                            Log.d("WeatherResponse", "API Response Success: $it")
                        } ?: run {
                            Log.e("WeatherResponse", "API Response Body is null")
                        }
                    } else {
                        Log.e("WeatherResponse", "API Response Not Successful: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<weathermodel>, t: Throwable) {
                    Log.e("WeatherResponse", "API Request Failure", t)
                }
            })
    }


    private fun requestPermissions(){

        ActivityCompat.requestPermissions(this,
        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION),LOCATION_REQUEST_CODE)

    }

    private fun isLocationEnaled(): Boolean{

        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE)
        as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun checkPermissions():Boolean{

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            ==PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            )== PackageManager.PERMISSION_GRANTED){

            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_REQUEST_CODE){

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                getCurrentLocation()
            }
            else{

            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setData(body:weathermodel){
        Log.d("WeatherData", "Humidity: ${body.main.humidity}, Wind Speed: ${body.wind.speed}")

        binding.apply {
            selectedLocation.text = body.name
            weatherTemp.text = ""+k2c(body?.main?.temp!!)+"°C"
            weatherState.text = body.weather[0].main
            weatherHumidity.text = body.main.humidity.toString()+"%"
            weatherWindSpeed.text = body.wind.speed.toString()+"m/s"
            weatherPressure.text = body.main.pressure.toString()+"hPa"

        }

        updateUI(body.weather[0].id)
    }

    private fun k2c(t:Double):Double{

        var intTemp = t
        intTemp = intTemp.minus(273)

        return intTemp.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()

    }

    private fun updateUI(id:Int){

        binding.apply {
            when(id){

                //Thunderstorm
                in 200..232->{

                    weatherLogo.setImageResource(R.drawable.thunderstorm)
                }

                //Drizzle
                in 300..321->{

                    weatherLogo.setImageResource(R.drawable.few_clouds)
                }
                //Rain
                in 500..531->{

                    weatherLogo.setImageResource(R.drawable.rain)
                }
                //Snow
                in 600..622->{

                    weatherLogo.setImageResource(R.drawable.snow)
                }
                //Atmosphere
                in 701..781->{

                    weatherLogo.setImageResource(R.drawable.broken_clouds)
                }
                //clear
                800->{

                    weatherLogo.setImageResource(R.drawable.clear_sky)
                }
                //Clouds
                in 801..804->{

                    weatherLogo.setImageResource(R.drawable.scattered_clouds)
                }
                //unknown
                else->{

                    weatherLogo.setImageResource(R.drawable.ic_unknown)
                }
            }
        }
    }
}