package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.example.myapplication.databinding.ActivitySetReferenceBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.UnknownHostException

class SetReferenceActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetReferenceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetReferenceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val apiKey = preferences.getString("api_key", "No API Key Set")
        binding.apiKeyTextView.text = apiKey

        binding.lightSlider.addOnChangeListener { _, value, _ ->
            binding.lightLabel.text = "Эталон освещенности: ${value.toInt()}"
        }

        binding.humiditySlider.addOnChangeListener { _, value, _ ->
            binding.humidityLabel.text = "Эталон влажности: ${value.toInt()}%"
        }

        binding.temperatureSlider.addOnChangeListener { _, value, _ ->
            binding.temperatureLabel.text = "Эталон температуры: ${value.toInt()}°C"
        }

        binding.changeApiKeyButton.setOnClickListener {
            val intent = Intent(this, ChangeApiKeyActivity::class.java)
            startActivity(intent)
        }

        binding.setReferences.setOnClickListener {
            sendReferenceValues(apiKey ?: "")
        }

        val currentApiKey = preferences.getString("api_key", "")
        if (!currentApiKey.isNullOrEmpty()) {
            fetchGreenhouseData(currentApiKey)
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

    private fun fetchGreenhouseData(apiKey: String) {
        RetrofitClient.api.getGreenhouseData(apiKey).enqueue(object : Callback<GreenhouseData> {
            override fun onResponse(
                call: Call<GreenhouseData>,
                response: Response<GreenhouseData>
            ) {
                if (response.isSuccessful) {
                    val data = response.body()
                    if (data != null) {
                        binding.currentLightTextView.text =
                            "Текущая освещенность: ${data.sensors[0].reading}°C"
                        binding.currentHumidityTextView.text =
                            "Текущая влажность: ${data.sensors[1].reading}%"
                        binding.currentTemperatureTextView.text =
                            "Текущая температура: ${data.sensors[3].reading}}%"
                        binding.lightSlider.value = data.sensors[0].reference;
                        binding.humiditySlider.value = data.sensors[1].reference;
                        binding.temperatureSlider.value = data.sensors[2].reference;
                    } else {
                        Toast.makeText(
                            this@SetReferenceActivity,
                            "No data received",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@SetReferenceActivity,
                        "Failed to fetch data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<GreenhouseData>, t: Throwable) {
                if (t is UnknownHostException) {
                    Toast.makeText(
                        this@SetReferenceActivity,
                        "Error: Unable to connect to server. Check the server address.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@SetReferenceActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    private fun sendReferenceValues(apiKey: String) {
        val referenceData = GreenhouseData(
            sensors = listOf(
                Sensor(1, "light", binding.lightSlider.value, 0f),
                Sensor(2, "moist", binding.humiditySlider.value, 0f),
                Sensor(3, "temperature", binding.temperatureSlider.value, 0f)
            )
        )

        RetrofitClient.api.updateGreenhouseData(apiKey, referenceData)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@SetReferenceActivity,
                            "Эталонные значения установлены",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@SetReferenceActivity,
                            "Не удалось установить эталонные значения",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    if (t is UnknownHostException) {
                        Toast.makeText(
                            this@SetReferenceActivity,
                            "Error: Unable to connect to server. Check the server address.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@SetReferenceActivity,
                            "Error: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
    }
}
