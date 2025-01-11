package com.example.myapplication.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.example.myapplication.LayoutUtils.MySwipeRefreshLayout
import com.example.myapplication.NetworkUtils.RetrofitClient
import com.example.myapplication.R
import com.example.myapplication.data.GreenhouseData
import com.example.myapplication.data.SensorReference
import com.example.myapplication.databinding.ActivitySetReferenceBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.UnknownHostException

class SetReferenceActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetReferenceBinding

    private var lightSensorId: Int? = null
    private var humiditySensorId: Int? = null
    private var temperatureSensorId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetReferenceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val swipeRefreshLayout = findViewById<MySwipeRefreshLayout>(R.id.swipeRefreshLayout)

        swipeRefreshLayout.addExcludedView(binding.lightReferenceSlider)
        swipeRefreshLayout.addExcludedView(binding.humidityReferenceSlider)
        swipeRefreshLayout.addExcludedView(binding.temperatureReferenceSlider)

        binding.swipeRefreshLayout.setOnRefreshListener {
            val deviceId = getCurrentId()
            if (!deviceId.isNullOrEmpty()) {
                fetchGreenhouseData(deviceId)
            } else {
                showToast("ID теплицы не установлен")
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        val deviceId = getCurrentId()
        binding.deviceIdValue.text = deviceId ?: ""

        binding.lightReferenceSlider.addOnChangeListener { _, value, _ ->
            binding.lightReferenceValue.text = "${value.toInt()}"
        }

        binding.humidityReferenceSlider.addOnChangeListener { _, value, _ ->
            binding.humidityReferenceValue.text = "${value.toInt()}%"
        }

        binding.temperatureReferenceSlider.addOnChangeListener { _, value, _ ->
            binding.temperatureReferenceValue.text = "${value.toInt()}°C"
        }

        binding.changeDeviceIdButton.setOnClickListener {
            startActivity(Intent(this, ChangeDeviceIdActivity::class.java))
        }

        binding.setReferences.setOnClickListener {
            if (deviceId != null) {
                sendReferenceValues()
            } else {
                showToast("Id теплицы не установлен")
            }
        }
    }


    override fun onResume() {
        super.onResume()
        updateCurrentId()
    }

    private fun updateCurrentId() {
        val deviceId = getCurrentId()
        binding.deviceIdValue.text = deviceId ?: ""
        if (!deviceId.isNullOrEmpty()) {
            fetchGreenhouseData(deviceId)
        }
    }

    private fun getCurrentId(): String? {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        return preferences.getString("device_id", null)
    }

    private fun fetchGreenhouseData(deviceId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.getGreenhouseData(deviceId)
                withContext(Dispatchers.Main) {
                    updateUiWithGreenhouseData(response)
                    binding.swipeRefreshLayout.isRefreshing = false

                }
            } catch (e: Exception) {
                handleNetworkError(e)
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun setAllDataToUnknown() {
        binding.currentLightValue.setText(R.string.NoData)
        binding.currentTemperatureValue.setText(R.string.NoData)
        binding.currentHumidityValue.setText(R.string.NoData)
    }

    private fun updateUiWithGreenhouseData(data: GreenhouseData) {
        binding.currentLightValue.text = "${data.sensors[0].reading}%"
        binding.currentTemperatureValue.text = "${data.sensors[1].reading}°C"
        binding.currentHumidityValue.text = "${data.sensors[2].reading}%"

        binding.lightReferenceSlider.value = data.sensors[0].reference
        binding.temperatureReferenceSlider.value = data.sensors[1].reference
        binding.humidityReferenceSlider.value = data.sensors[2].reference

        updateSensorsIdFields(data)
    }

    private fun updateSensorsIdFields(data: GreenhouseData) {
        lightSensorId = data.sensors[0].id
        temperatureSensorId = data.sensors[1].id
        humiditySensorId = data.sensors[2].id
    }

    private fun sendReferenceValues() {
        val lightSensor = SensorReference( binding.lightReferenceSlider.value)
        val humiditySensor = SensorReference(binding.humidityReferenceSlider.value)
        val temperatureSensor = SensorReference(binding.temperatureReferenceSlider.value)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                RetrofitClient.api.updateGreenhouseData(lightSensorId, lightSensor)
                RetrofitClient.api.updateGreenhouseData(humiditySensorId, humiditySensor)
                RetrofitClient.api.updateGreenhouseData(temperatureSensorId, temperatureSensor)
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
            is HttpException -> {
                if (e.code() == 404) {
                    "Не удалось найти теплицу с данным ID"
                } else {
                    "Сетевая ошибка: ${e.code()}"
                }
            }
            else -> "Ошибка: ${e.localizedMessage}"
        }
        lifecycleScope.launch(Dispatchers.Main) {
            showToast(message)
            setAllDataToUnknown()
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

