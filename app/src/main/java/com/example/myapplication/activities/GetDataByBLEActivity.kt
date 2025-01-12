package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import com.example.myapplication.databinding.ActivityGetDataBinding
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

class GetDataByBLEActivity : BaseBleActivity() {

    private lateinit var binding: ActivityGetDataBinding

    override val serviceUUID: UUID = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb")
    override val characteristicUUID: UUID = UUID.fromString("00002A57-0000-1000-8000-00805f9b34fb")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGetDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.sendWifiDataButton.setOnClickListener() {
            val intent = Intent(this, SendWifiDataActivity::class.java)
            startActivity(intent)
        }

        binding.refreshButton.setOnClickListener {
            if (isDeviceReady) {
                sendData("""{"command": "getData"}""", requiresResponse = true)
            } else {
                showToast("Устройство ещё не подключено")
            }
        }

        binding.sendNewRefButton.setOnClickListener() {
            val intent = Intent(this, SetReferencesByBluetoothActivity::class.java)
            startActivity(intent)
        }


        binding.changeBLEDevice.setOnClickListener() {
            if (!scanning) {
                deleteDevice()
                startScan()
            } else {
                stopScan()
            }
            binding.refreshButton.performClick()
        }
    }

    override fun onResume() {
        super.onResume()
        reconnectToDevice()
        Thread.sleep(160)
    }


    @SuppressLint("SetTextI18n")
    override fun processCharacteristicValue(value: ByteArray) {
        val jsonString = String(value, Charsets.UTF_8)
        try {
            val jsonObject = JSONObject(jsonString)

            val sensors = jsonObject.getJSONArray("sensors")
            val actuators = jsonObject.getJSONArray("actuators")

            val lightSensor = sensors.getJSONObject(0)
            val tempSensor = sensors.getJSONObject(1)
            val moistureSensor = sensors.getJSONObject(2)

            val lamp = actuators.getJSONObject(0)
            val cooler = actuators.getJSONObject(1)
            val heater = actuators.getJSONObject(2)
            val shower = actuators.getJSONObject(3)

            val lightText =
                "${lightSensor.getString("reading")}/${lightSensor.getString("reference")}"
            val tempText = "${tempSensor.getString("reading")}/${tempSensor.getString("reference")}"
            val moistureText =
                "${moistureSensor.getString("reading")}/${moistureSensor.getString("reference")}"
            val lampStatus = if (lamp.getBoolean("status")) "🟢" else "⚪️"
            val coolerStatus = if (cooler.getBoolean("status")) "🟢" else "⚪️"
            val heaterStatus = if (heater.getBoolean("status")) "🟢" else "⚪️"
            val showerStatus = if (shower.getBoolean("status")) "🟢" else "⚪️"

            binding.lightValue.text = lightText
            binding.temperatureValue.text = tempText
            binding.moistureValue.text = moistureText
            binding.lightActuatorState.text = lampStatus
            binding.coolingState.text = coolerStatus
            binding.heatState.text = heaterStatus
            binding.wateringState.text = showerStatus
        } catch (e: JSONException) {
            runOnUiThread {
                showToast("Ошибка обработки данных ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDeviceReady() {
        sendData("""{"command": "getData"}""", requiresResponse = true)
    }
}

