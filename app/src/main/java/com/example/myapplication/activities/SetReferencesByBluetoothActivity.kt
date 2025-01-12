package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.os.Bundle
import com.example.myapplication.databinding.ActivitySetReferencesByBluetoothBinding
import java.util.UUID


class SetReferencesByBluetoothActivity : BaseBleActivity() {
    override val serviceUUID: UUID = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb")
    override val characteristicUUID: UUID = UUID.fromString("00002A57-0000-1000-8000-00805f9b34fb")

    private lateinit var binding: ActivitySetReferencesByBluetoothBinding


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetReferencesByBluetoothBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reconnectToDevice()


        binding.lightReferenceSlider.addOnChangeListener { _, value, _ ->
            binding.lightReferenceValue.text = "${value.toInt()}%"
        }

        binding.humidityReferenceSlider.addOnChangeListener { _, value, _ ->
            binding.humidityReferenceValue.text = "${value.toInt()}%"
        }

        binding.temperatureReferenceSlider.addOnChangeListener { _, value, _ ->
            binding.temperatureReferenceValue.text = "${value.toInt()}°C"
        }

        binding.setReferences.setOnClickListener {
            if (selectedDevice != null) {
                sendData(getDataToSend(binding.lightReferenceSlider.value.toInt(),
                    binding.temperatureReferenceSlider.value.toInt(),
                    binding.humidityReferenceSlider.value.toInt()))
                showToast("Значения отправлены")
            } else {
                showToast("Сначала выберите устройство")
            }
        }
    }

    override fun processCharacteristicValue(value: ByteArray) {
    }

    private fun getDataToSend(lightValue: Int, tempValue: Int, moistureValue: Int): String {
        return "{\"command\": \"setLocalReferences\", \"light\": ${lightValue}, \"temp\": ${tempValue}, \"moisture\": ${moistureValue}}"
    }
}

