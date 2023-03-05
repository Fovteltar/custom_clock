package com.example.customclocks

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.customclocks.databinding.ActivityMainBinding

class MainActivity: AppCompatActivity(R.layout.activity_main) {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
    }
}