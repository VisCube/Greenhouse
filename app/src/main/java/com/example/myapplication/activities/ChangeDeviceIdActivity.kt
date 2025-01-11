package com.example.myapplication.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.example.myapplication.databinding.ActivityChangeDeviceIdBinding

class ChangeDeviceIdActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangeDeviceIdBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChangeDeviceIdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val currentDeviceId = sharedPreferences.getString("device_id", "")
        binding.editDeviceId.setText(currentDeviceId)

        binding.btnSave.setOnClickListener {
            val newDeviceId = binding.editDeviceId.text.toString().trim()

            if (newDeviceId.isEmpty()) {
                Toast.makeText(this, "Введите ID теплицы", Toast.LENGTH_SHORT).show()
            } else {
                sharedPreferences.edit().putString("device_id", newDeviceId).apply()

                Toast.makeText(this, "ID успешно обовлен", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}