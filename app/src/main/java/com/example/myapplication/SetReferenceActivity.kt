package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.example.myapplication.databinding.ActivitySetReferenceBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.UnknownHostException

class SetReferenceActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetReferenceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetReferenceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val apiKey = getApiKey()
        binding.apiKeyValue.text = apiKey ?: ""

        binding.lightReferenceSlider.addOnChangeListener { _, value, _ ->
            binding.lightReferenceValue.text = "${value.toInt()}"
        }

        binding.humidityReferenceSlider.addOnChangeListener { _, value, _ ->
            binding.humidityReferenceValue.text = "${value.toInt()}%"
        }

        binding.temperatureReferenceSlider.addOnChangeListener { _, value, _ ->
            binding.temperatureReferenceValue.text = "${value.toInt()}°C"
        }

        binding.changeApiKeyButton.setOnClickListener {
            startActivity(Intent(this, ChangeApiKeyActivity::class.java))
        }

        binding.setReferences.setOnClickListener {
            if (apiKey != null) {
                sendReferenceValues(apiKey)
            } else {
                showToast("API ключ не установлен")
            }
        }
    }


    override fun onResume() {
        super.onResume()
        updateApiKey()
    }

    private fun updateApiKey() {
        val apiKey = getApiKey()
        binding.apiKeyValue.text = apiKey ?: ""
        if (!apiKey.isNullOrEmpty()) {
            fetchGreenhouseData(apiKey)
        }
    }

    private fun getApiKey(): String? {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        return preferences.getString("api_key", null)
    }

    private fun fetchGreenhouseData(apiKey: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.getGreenhouseData()
                withContext(Dispatchers.Main) {
                    updateUiWithGreenhouseData(response)
                }
            } catch (e: Exception) {
                handleNetworkError(e)
            }
        }
    }

    private fun updateUiWithGreenhouseData(data: GreenhouseData) {
        binding.currentLightValue.text = "${data.sensors[0].reading}lm"
        binding.currentHumidityValue.text = "${data.sensors[1].reading}%"
        binding.currentTemperatureValue.text = "${data.sensors[2].reading}°C"

        binding.lightReferenceSlider.value = data.sensors[0].reference
        binding.humidityReferenceSlider.value = data.sensors[1].reference
        binding.temperatureReferenceSlider.value = data.sensors[2].reference
    }

    private fun sendReferenceValues(apiKey: String) {
        val referenceData = GreenhouseReferences(
            sensors = listOf(
                SensorReference(1, binding.lightReferenceSlider.value),
                SensorReference(2, binding.humidityReferenceSlider.value),
                SensorReference(3, binding.temperatureReferenceSlider.value)
            )
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                RetrofitClient.api.updateGreenhouseData(referenceData)
                withContext(Dispatchers.Main) {
                    showToast("Эталонные значения установлены")
                }
            } catch (e: Exception) {
                handleNetworkError(e)
            }
        }
    }

    private fun handleNetworkError(e: Exception) {
        val message = when (e) {
            is UnknownHostException -> "Не удалось связаться с сервером"
            is HttpException -> "Сетевая ошибка: ${e.response()?.code()}"
            else -> "Ошибка: ${e.localizedMessage}"
        }
        lifecycleScope.launch(Dispatchers.Main) {
            showToast(message)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

