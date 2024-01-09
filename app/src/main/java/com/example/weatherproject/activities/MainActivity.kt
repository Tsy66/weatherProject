package com.example.weatherproject.activities

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.example.weatherproject.R
import com.example.weatherproject.databinding.ActivityMainBinding
import com.example.weatherproject.models.AirModel
import com.example.weatherproject.models.Components
import com.example.weatherproject.models.Constants
import com.example.weatherproject.models.ForecastModel
import com.example.weatherproject.models.WeatherModel
import com.example.weatherproject.utilities.ApiUtilities
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var currentLocation: Location
    private lateinit var fuseLocationProvider: FusedLocationProviderClient
    private val LOCATION_REQUEST_CODE=101
    private var lastLat = 0.0
    private var lastLon = 0.0
    private var lastCityName = ""
    private lateinit var cityAutoComplete: AutoCompleteTextView
    private lateinit var dropDownAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var sharedPreferences: SharedPreferences
        sharedPreferences = getSharedPreferences("NightMode", MODE_PRIVATE)

        val isNightModeOn = sharedPreferences.getBoolean("NightMode", true)
        if (isNightModeOn) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        setContentView(R.layout.activity_main)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        fuseLocationProvider = LocationServices.getFusedLocationProviderClient(this)

        getCurrentLocation()

        cityAutoComplete = findViewById(R.id.location_search)

        cityAutoComplete.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                dropDownAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, loadSearchHistory())
                cityAutoComplete.setAdapter(dropDownAdapter)
                if (loadSearchHistory().isNotEmpty()) {
                    cityAutoComplete.post {
                        cityAutoComplete.showDropDown()
                    }
                }
            }
        }

        cityAutoComplete.setOnEditorActionListener { textView, i, keyEvent ->

            if (i == EditorInfo.IME_ACTION_SEARCH) {
                getCityWeather(cityAutoComplete.text.toString())
                cityAutoComplete.text.clear()
                val view = this.currentFocus
                if (view!=null) {
                    val imm:InputMethodManager=getSystemService(INPUT_METHOD_SERVICE)
                            as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    cityAutoComplete.clearFocus()
                }
                return@setOnEditorActionListener true
            } else {
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

        binding.refreshContent.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            fetchCurrentLocationWeather(lastLat.toString(), lastLon.toString())
            Log.d("Refresh Location", "Latitude: ${lastLat}, Longitude: ${lastLon}")
        }

        binding.settingsContent.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.back.setOnClickListener {
            hideKeyboard(this)

            binding.locationOptionBar.visibility = View.VISIBLE
            binding.locationSearchBar.visibility = View.GONE
        }

        binding.favoriteContent.setOnClickListener {
            showSaveFavoriteDialog(lastCityName)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.SCHEDULE_EXACT_ALARM), 100)
            }
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
            if (isLocationEnabled()) {
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
                            lastCityName = ""
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

        ApiUtilities.getApiInterface()?.getCityWeatherData(city, Constants.apiKey, Constants.lang, Constants.units)
            ?.enqueue(object : Callback<WeatherModel>{

                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<WeatherModel>,
                    response: Response<WeatherModel>
                ) {

                    if (response.isSuccessful){

                        binding.locationOptionBar.visibility = View.VISIBLE
                        binding.locationSearchBar.visibility = View.GONE
                        binding.progressBar.visibility = View.GONE

                        response.body()?.let{
                            setData(it)
                        }
                        //搜尋成功才會存紀錄
                        saveSearchHistory(city)
                        lastCityName = city
                    }
                    else{

                        binding.locationOptionBar.visibility = View.VISIBLE
                        binding.locationSearchBar.visibility = View.GONE
                        binding.progressBar.visibility = View.GONE

                        Toast.makeText(this@MainActivity, "No City Found", Toast.LENGTH_SHORT).show()

                    }
                }

                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {

                }


            })
    }

    private fun fetchCurrentLocationWeather(latitude: String, longitude: String) {
        ApiUtilities.getApiInterface()?.getCurrentWeatherData(latitude, longitude, Constants.apiKey, Constants.lang, Constants.units)
            ?.enqueue(object : Callback<WeatherModel> {

                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<WeatherModel>,
                    response: Response<WeatherModel>
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

                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {
                    Log.e("WeatherResponse", "API Request Failure", t)
                }
            })
    }

    //用lat 跟 lon 判斷位置的
    private fun getAirPollution(latitude: String, longitude: String) {
        ApiUtilities.getApiInterface()?.getCurrentAirPollution(latitude, longitude, Constants.apiKey)
            ?.enqueue(object : Callback<AirModel> {

                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<AirModel>,
                    response: Response<AirModel>
                ) {

                    if (response.isSuccessful) {
                        response.body()?.let {
                            setAirData(it)
                            Log.d("AirModel", "API Response Success: $it")
                        } ?: run {
                            Log.e("AirModel", "API Response Body is null")
                        }
                    } else {
                        Log.e("AirModel", "API Response Not Successful: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<AirModel>, t: Throwable) {
                    Log.e("AirModel", "API Request Failure", t)
                }
            })
    }

    private fun requestPermissions(){

        ActivityCompat.requestPermissions(this,
        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION),LOCATION_REQUEST_CODE)

    }

    private fun isLocationEnabled(): Boolean{

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
    private fun setData(body:WeatherModel){
        Log.d("WeatherData", "Humidity: ${body.main.humidity}, Wind Speed: ${body.wind.speed}")

        binding.apply {
            selectedLocation.text = body.name //city name
            //weatherTemp.text = ""+k2c(body?.main?.temp!!)+"°C"
            weatherTemp.text = (Math.round(body.main.temp * 10.0) / 10.0).toString()+"°C"
            weatherState.text = body.weather[0].description
            weatherHumidity.text = body.main.humidity.toString()+"%"
            weatherWindSpeed.text = body.wind.speed.toString()+"m/s"
            weatherPressure.text = body.main.pressure.toString()+"hPa"
            weatherFeelsLike.text = (Math.round(body.main.feels_like * 10.0) / 10.0).toString()+"°C"
            weatherVisibility.text = ""+Math.round(body.visibility / 1000.0 * 100.0) * 0.01+"km"
            //weatherFeelsLike.text = ""+k2c(body?.main?.feels_like!!)+"°C"
            //空氣指數
            lastLat = body.coord.lat
            lastLon = body.coord.lon
            getAirPollution(lastLat.toString(), lastLon.toString())
            getForecastWeather(lastLat.toString(), lastLon.toString())
        }

        updateUI(body.weather[0].id)
    }

    private fun setAirData(body:AirModel) {
        //var comp: Components
        //comp = body.list[0].components
        //val values = listOf(comp.co, comp.no, comp.no2, comp.o3, comp.so2, comp.pm2_5, comp.pm10, comp.nh3)
        //val aqi = values.maxOrNull()

        Log.d("AirInfo", "空氣指數: ${body.list[0].main.aqi}")

        binding.apply {
            val myImage: ImageView = findViewById(R.id.air_image)
            when (body.list[0].main.aqi) {
                1 -> {
                    myImage.setImageResource(R.drawable.air1)
                    weatherAirPollution.text = "優"
                }
                2 -> {
                    myImage.setImageResource(R.drawable.air2)
                    weatherAirPollution.text = "良"
                }
                3 -> {
                    myImage.setImageResource(R.drawable.air3)
                    weatherAirPollution.text = "普通"
                }
                4 -> {
                    myImage.setImageResource(R.drawable.air4)
                    weatherAirPollution.text = "不好"
                }
                5 -> {
                    myImage.setImageResource(R.drawable.air5)
                    weatherAirPollution.text = "糟糕"
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setForcastData(body:ForecastModel) {
        val forecastHourLayer = findViewById<LinearLayout>(R.id.forecastHourLayer)
        forecastHourLayer.removeAllViews() // Clear the mainLinearLayout

        val forecastDayLayer = findViewById<LinearLayout>(R.id.forecastDayLayer)
        forecastDayLayer.removeAllViews() // Clear the mainLinearLayout
        val totalWeight = body.list.size
        var i = 0
        //顯示10個就好
        val currentTime = Calendar.getInstance().timeInMillis
        for (forecast in body.list) {
            val forecastTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(forecast.dt_txt).time
            if (currentTime >= forecastTime)
                continue
            if (i == 10)
                break
            val linearLayout = LinearLayout(this)
            linearLayout.layoutParams = LinearLayout.LayoutParams(420, 600) // Set fixed size
            linearLayout.orientation = LinearLayout.VERTICAL
            linearLayout.gravity = Gravity.CENTER
            linearLayout.setPadding(0, 0, 10, 0) // Add padding to the right side of the LinearLayout
            forecastHourLayer.addView(linearLayout)

            val timeTextView = TextView(this)
            timeTextView.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            timeTextView.text = forecast.dt_txt.substring(11,16)
            timeTextView.setPadding(0,25,0,25)
            linearLayout.addView(timeTextView)

            val imageView = ImageView(this)
            imageView.layoutParams = LinearLayout.LayoutParams(200, 200)
            //imageView.setImageResource(R.drawable.air1) // Replace 'your_image' with your actual image resource
            imageView.setPadding(0,50,0,50)
            updateForecastUI(imageView ,forecast.weather[0].id)
            linearLayout.addView(imageView)

            val rainProbRelativeLayout = RelativeLayout(this)
            rainProbRelativeLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            rainProbRelativeLayout.gravity = Gravity.CENTER // Center the RelativeLayout
            linearLayout.addView(rainProbRelativeLayout)

            val rainProbIcon = ImageView(this)
            rainProbIcon.layoutParams = RelativeLayout.LayoutParams(70, 70)
            rainProbIcon.setImageResource(R.drawable.rain_probability) // Replace 'your_icon' with your actual icon resource
            rainProbIcon.id = View.generateViewId()
            rainProbRelativeLayout.addView(rainProbIcon)

            val rainProbTextView = TextView(this)
            rainProbTextView.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            rainProbTextView.text = ""+(Math.round(forecast.pop * 100.0))+"%"
            rainProbTextView.id = View.generateViewId()
            rainProbTextView.setPadding(10, 0, 0, 0) // Add padding below the TextView
            rainProbRelativeLayout.addView(rainProbTextView)

            // Set the position of the precipitationTextView to be on the right side of the precipitationIcon
            val params = rainProbTextView.layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.RIGHT_OF, rainProbIcon.id)
            rainProbTextView.layoutParams = params

            val tempTextView = TextView(this)
            tempTextView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            tempTextView.text = "${Math.round(forecast.main.temp * 10.0) / 10.0}°C"
            tempTextView.setPadding(0, 50, 0, 0) // Add padding below the TextView
            tempTextView.gravity = Gravity.CENTER
            linearLayout.addView(tempTextView)
            i++
        }
        //最高/低溫度 降雨機率 每日
        val dailyData = mutableMapOf<String, MutableMap<String, Any>>()

        for (forecast in body.list) {
            val dateTime = forecast.dt_txt.split(" ")[0]
            if (!dailyData.containsKey(dateTime)) {
                dailyData[dateTime] = mutableMapOf("high_temp" to Double.NEGATIVE_INFINITY, "low_temp" to Double.POSITIVE_INFINITY, "rain_prob" to 0.0, "count" to 0, "weather_id_count" to mutableMapOf<Int, Int>())
            }
            val highTemp = forecast.main.temp_max
            val lowTemp = forecast.main.temp_min
            val rainProb = forecast.pop
            val weatherId = forecast.weather[0].id / 100

            if (highTemp > dailyData[dateTime]!!["high_temp"] as Double) {
                dailyData[dateTime]!!["high_temp"] = highTemp
            }
            if (lowTemp < dailyData[dateTime]!!["low_temp"] as Double) {
                dailyData[dateTime]!!["low_temp"] = lowTemp
            }

            // Update the rainfall probability and count
            dailyData[dateTime]!!["rain_prob"] = (dailyData[dateTime]!!["rain_prob"] as Double + rainProb)
            dailyData[dateTime]!!["count"] = (dailyData[dateTime]!!["count"] as Int + 1)
            // Update the weather id count
            if (!(dailyData[dateTime]!!["weather_id_count"] as MutableMap<Int, Int>).containsKey(weatherId)) {
                (dailyData[dateTime]!!["weather_id_count"] as MutableMap<Int, Int>)[weatherId] = 0
            }
            (dailyData[dateTime]!!["weather_id_count"] as MutableMap<Int, Int>)[weatherId] = (dailyData[dateTime]!!["weather_id_count"] as MutableMap<Int, Int>)[weatherId]!! + 1

            Log.d("ForecastText", dateTime+"Test "+dailyData[dateTime]!!["count"]+"")
        }

        val today = LocalDate.now().toString()
        for ((dtTxt, info) in dailyData) {
            val sdf = SimpleDateFormat("yyyy-MM-dd")
            val dateTime = sdf.parse(dtTxt.split(" ")[0])
            val todayDate = sdf.parse(today)
            if (dateTime.compareTo(todayDate) <= 0) {
                continue
            }
            val linearLayout = LinearLayout(this)
            linearLayout.layoutParams = LinearLayout.LayoutParams(420, 900) // Set fixed size
            linearLayout.orientation = LinearLayout.VERTICAL
            linearLayout.gravity = Gravity.CENTER
            linearLayout.setPadding(0, 0, 10, 0) // Add padding to the right side of the LinearLayout
            forecastDayLayer.addView(linearLayout)

            val timeTextView = TextView(this)
            timeTextView.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            timeTextView.text = dtTxt.substring(5, 10)
            timeTextView.setPadding(0,25,0,25)
            linearLayout.addView(timeTextView)

            val weatherIdCounts = info["weather_id_count"] as MutableMap<Int, Int>
            val mostFrequentWeatherId = (weatherIdCounts.entries.maxByOrNull { it.value }?.key ?: 0) * 100

            val imageView = ImageView(this)
            imageView.layoutParams = LinearLayout.LayoutParams(200, 200)
            //imageView.setImageResource(R.drawable.air1) // Replace 'your_image' with your actual image resource
            imageView.setPadding(0,50,0,50)
            updateForecastUI(imageView, mostFrequentWeatherId)
            linearLayout.addView(imageView)

            val rainProbRelativeLayout = RelativeLayout(this)
            rainProbRelativeLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            rainProbRelativeLayout.gravity = Gravity.CENTER // Center the RelativeLayout
            linearLayout.addView(rainProbRelativeLayout)

            val rainProbIcon = ImageView(this)
            rainProbIcon.layoutParams = RelativeLayout.LayoutParams(70, 70)
            rainProbIcon.setImageResource(R.drawable.rain_probability) // Replace 'your_icon' with your actual icon resource
            rainProbIcon.id = View.generateViewId()
            rainProbRelativeLayout.addView(rainProbIcon)

            info["rain_prob"] = (info["rain_prob"] as Double / info["count"] as Int)

            val rainProbTextView = TextView(this)
            rainProbTextView.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            rainProbTextView.text = ""+(Math.round((info["rain_prob"] as Double) * 100.0))+"%"
            rainProbTextView.id = View.generateViewId()
            rainProbTextView.setPadding(10, 0, 0, 0) // Add padding below the TextView
            rainProbRelativeLayout.addView(rainProbTextView)

            // Set the position of the precipitationTextView to be on the right side of the precipitationIcon
            val params = rainProbTextView.layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.RIGHT_OF, rainProbIcon.id)
            rainProbTextView.layoutParams = params

            // Create TextViews for the highest and lowest temperatures
            val highTempTextView = TextView(this)
            highTempTextView.text = "${Math.round(info["high_temp"] as Double * 10.0) / 10.0}°C"
            highTempTextView.id = View.generateViewId()
            highTempTextView.gravity = Gravity.CENTER
            highTempTextView.setPadding(0, 50, 0, 50) // Add bottom padding
            linearLayout.addView(highTempTextView)

            // Create a CardView for the thermometer
            val thermometerCardView = CardView(this)
            thermometerCardView.layoutParams = RelativeLayout.LayoutParams(25, 150)
            thermometerCardView.radius = 30f // Set the corner radius
            val purpleColor = ContextCompat.getColor(this, R.color.purple_700)
            thermometerCardView.setCardBackgroundColor(purpleColor)
            thermometerCardView.id = View.generateViewId()
            linearLayout.addView(thermometerCardView)

            val lowTempTextView = TextView(this)
            lowTempTextView.text = "${Math.round(info["low_temp"] as Double * 10.0) / 10.0}°C"
            lowTempTextView.id = View.generateViewId()
            lowTempTextView.gravity = Gravity.CENTER
            lowTempTextView.setPadding(0, 50, 0, 0) // Add top padding
            linearLayout.addView(lowTempTextView)
        }
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

    private fun updateForecastUI(weatherLogo: ImageView, id:Int){

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

    //Forecast Weather
    private fun getForecastWeather(latitude: String, longitude: String) {
        ApiUtilities.getApiInterface()?.getForecastWeatherData(latitude, longitude, Constants.apiKey, Constants.lang, Constants.units)
            ?.enqueue(object : Callback<ForecastModel> {

                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<ForecastModel>,
                    response: Response<ForecastModel>
                ) {

                    if (response.isSuccessful) {
                        response.body()?.let {
                            setForcastData(it)
                            Log.d("ForecastModel", "API Response Success: $it")
                        } ?: run {
                            Log.e("ForecastModel", "API Response Body is null")
                        }
                    } else {
                        Log.e("ForecastModel", "API Response Not Successful: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ForecastModel>, t: Throwable) {
                    Log.e("ForecastModel", "API Request Failure", t)
                }
            })
    }

    fun loadSearchHistory(): MutableList<String> {
        val sharedPreferences = getSharedPreferences("searchHistory", Context.MODE_PRIVATE)
        var result: Set<String> = sharedPreferences.getStringSet("cities", HashSet()) ?: HashSet()
        return result.toMutableList()
    }

    fun saveSearchHistory(city: String) {
        val sharedPreferences = getSharedPreferences("searchHistory", Context.MODE_PRIVATE)
        var searchHistory = sharedPreferences.getStringSet("cities", HashSet())
        if (searchHistory == null) {
            searchHistory = HashSet()
        } else {
            searchHistory = HashSet(searchHistory)
        }
        searchHistory.add(city)
        val editor = sharedPreferences.edit()
        editor.putStringSet("cities", searchHistory)
        editor.apply()
        //dropDownAdapter.notifyDataSetChanged()
    }

    private lateinit var alertDialog: AlertDialog

    private fun showSaveFavoriteDialog(cityName: String) {
        val builder = AlertDialog.Builder(this)

        // 獲取已保存的我的最愛
        val favoriteCities = getFavoriteCities()

        // 創建一個自定義的 Layout
        val layoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(R.layout.custom_dialog_layout, null)

        var cityListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, favoriteCities)
        val cityListView = view.findViewById<ListView>(R.id.cityListView)
        cityListView.adapter = cityListAdapter
        cityListView.isVerticalScrollBarEnabled = true
        // 如果列表為空，則設定 ListView 的高度為 100dp
        if (favoriteCities.isEmpty()) {
            cityListView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(25))
        } else {
            cityListView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(300))
        }


        cityListView.setOnItemClickListener { _, _, position, _ ->
            val selectedCity = cityListAdapter.getItem(position) as String
            getCityWeather(selectedCity)
            alertDialog.dismiss()
        }

        cityListView.setOnItemLongClickListener { _, _, position, _ ->
            val selectedCity = cityListAdapter.getItem(position) as String

            // 顯示刪除確認對話框
            AlertDialog.Builder(this)
                .setTitle("確認刪除")
                .setMessage("是否刪除 $selectedCity？")
                .setPositiveButton("確定") { _, _ ->
                    favoriteCities.toMutableList().remove(selectedCity)

                    // 從 SharedPreferences 中移除相應的城市
                    val sharedPreferences = getSharedPreferences("favorites", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.remove("$selectedCity")
                    editor.apply()

                    alertDialog.dismiss()
                }
                .setNegativeButton("取消", null)
                .show()
            true
        }

        if (cityName.isEmpty()) {
            val emptyTextView = view.findViewById<TextView>(R.id.titleTextView)
            emptyTextView.text = "目前沒有輸入任何城市名稱"
            emptyTextView.gravity = Gravity.CENTER
            emptyTextView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            emptyTextView.setPadding(dpToPx(10), dpToPx(30), 0, dpToPx(20))
        }

        // 檢查城市是否已經在我的最愛中
        val isFavorited = favoriteCities.contains(cityName)

        if (!cityName.isEmpty() && !isFavorited) {
            val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
            titleTextView.text = "將此城市添加到我的最愛？"
            titleTextView.gravity = Gravity.START
            titleTextView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            titleTextView.setPadding(dpToPx(10), dpToPx(30), 0, dpToPx(10))
            builder.setPositiveButton("添加") { _, _ ->
                saveFavoriteCity(cityName)
            }
            builder.setNegativeButton("取消") { dialog, _ ->
                dialog.cancel()
            }
        }

        alertDialog = builder.setView(view).create()
        alertDialog.show()
    }

    fun dpToPx(dp: Int): Int {
        val density = Resources.getSystem().displayMetrics.density
        return (dp * density).roundToInt()
    }

    private fun getFavoriteCities(): List<String> {
        val sharedPreferences = getSharedPreferences("favorites", Context.MODE_PRIVATE)
        return sharedPreferences.all.keys.filterIsInstance<String>()
    }

    private fun saveFavoriteCity(cityName: String) {
        val sharedPreferences = getSharedPreferences("favorites", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("$cityName", cityName)
        editor.apply()
    }
}