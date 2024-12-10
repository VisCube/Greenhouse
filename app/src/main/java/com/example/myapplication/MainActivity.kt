package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val apiKey = preferences.getString("api_key", "No API Key Set")
        binding.apiKeyTextView.text = apiKey

        binding.changeApiKeyButton.setOnClickListener {
            val intent = Intent(this, ChangeApiKeyActivity::class.java)
            startActivity(intent)
        }

        binding.setReferencesButton.setOnClickListener {
            val intent = Intent(this, SetReferenceActivity::class.java)
            startActivity(intent)
        }

        binding.sendWifiDataButton.setOnClickListener {
            val intent = Intent(this, SendWifiDataActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        updateApiKey()
    }

    private fun updateApiKey() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val apiKey = preferences.getString("api_key", "No API Key Set")
        binding.apiKeyTextView.text = apiKey;
    }

}
