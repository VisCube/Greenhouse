package com.example.myapplication

import BaseBleActivity
import android.annotation.SuppressLint
import android.os.Bundle
import com.example.myapplication.databinding.ActivitySetReferencesByBluetoothBinding
import java.util.UUID


class SetReferencesByBluetoothActivity : BaseBleActivity() {
    override val serviceUUID: UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc")
    override val characteristicUUID: UUID = UUID.fromString("87654321-4321-4321-4321-cba987654321")
    override fun getDeviceChosenValueId(): Int = R.id.deviceChosenValue

    private lateinit var binding: ActivitySetReferencesByBluetoothBinding


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetReferencesByBluetoothBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.lightReferenceSlider.addOnChangeListener { _, value, _ ->
            binding.lightReferenceValue.text = "${value.toInt()}%"
        }

        binding.humidityReferenceSlider.addOnChangeListener { _, value, _ ->
            binding.humidityReferenceValue.text = "${value.toInt()}%"
        }

        binding.temperatureReferenceSlider.addOnChangeListener { _, value, _ ->
            binding.temperatureReferenceValue.text = "${value.toInt()}°C"
        }

        binding.btnScanDevices.setOnClickListener {
            if (!scanning) {
                startScan()

            } else {
                stopScan()
            }
            binding.deviceChosenValue.text = selectedDevice?.name
        }

        binding.setReferences.setOnClickListener {
            if (selectedDevice != null) {
                sendData(getDataToSend())
            } else {
                showToast("Сначала выберите устройство")
            }
        }
    }

    private fun getDataToSend(): String {
        val lightValue = binding.lightReferenceSlider.value
        val humidityValue = binding.humidityReferenceSlider.value
        val temperatureValue = binding.temperatureReferenceSlider.value
        return "{\"command\": \"anotherCommand\", \"data\": \"$lightValue $humidityValue $temperatureValue\"}"
    }
}

