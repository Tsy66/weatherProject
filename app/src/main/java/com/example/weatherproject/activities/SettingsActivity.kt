package com.example.weatherproject.activities

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.weatherproject.R
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Calendar

class SettingsActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var reminderSwitch: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences("NightMode", MODE_PRIVATE)
        editor = sharedPreferences.edit()

        val switchBtn = findViewById<SwitchMaterial>(R.id.switchBtn)

        // Set initial state based on saved value
        val isNightModeOn = sharedPreferences.getBoolean("NightMode", true)
        switchBtn.isChecked = isNightModeOn

        if (isNightModeOn) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        switchBtn.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                editor.putBoolean("NightMode", true)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                editor.putBoolean("NightMode", false)
            }
            editor.apply()
        }

        // Clear search history
        val clearHistoryLayout = findViewById<CardView>(R.id.clearHistoryLayout)
        clearHistoryLayout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("確認")
                .setMessage("你確定要清除搜尋紀錄？")
                .setPositiveButton("Yes") { dialog, _ ->
                    val sharedPreferences = getSharedPreferences("searchHistory", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.clear()
                    editor.apply()
                    dialog.dismiss()
                }
                .setNegativeButton("No", null)
                .show()
        }

        // Clear favorites
        val clearFavoritesLayout = findViewById<CardView>(R.id.clearFavoritesLayout)
        clearFavoritesLayout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("確認")
                .setMessage("你確定要清除我的最愛？")
                .setPositiveButton("Yes") { dialog, _ ->
                    val sharedPreferences = getSharedPreferences("favorites", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.clear()
                    editor.apply()
                    dialog.dismiss()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }
}