package com.example.myapplication

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.example.myapplication.databinding.ActivityChangeApiKeyBinding

class ChangeApiKeyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangeApiKeyBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChangeApiKeyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val currentApiKey = sharedPreferences.getString("api_key", "")
        binding.etApiKey.setText(currentApiKey)

        binding.btnSave.setOnClickListener {
            val newApiKey = binding.etApiKey.text.toString().trim()

            if (newApiKey.isEmpty()) {
                Toast.makeText(this, "Please enter a valid API key", Toast.LENGTH_SHORT).show()
            } else {
                sharedPreferences.edit().putString("api_key", newApiKey).apply()

                Toast.makeText(this, "API key updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}