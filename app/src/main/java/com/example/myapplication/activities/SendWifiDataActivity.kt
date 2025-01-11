package com.example.myapplication.activities

import android.os.Bundle
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivitySendWifiDataBinding
import java.util.UUID


class SendWifiDataActivity : BaseBleActivity() {

    override val serviceUUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")
    override val characteristicUUID: UUID = UUID.fromString ("abcdef01-1234-5678-1234-56789abcdef0")
    override fun getDeviceChosenValueId(): Int = R.id.deviceChosenValue

    private lateinit var binding: ActivitySendWifiDataBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendWifiDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnScanDevices.setOnClickListener {
            if (!scanning) {
                startScan()
            } else {
                stopScan()
            }
        }

        binding.btnSendWifiData.setOnClickListener {
            if (selectedDevice != null) {
                val ssid = binding.editTextSsid.text.toString()
                val password = binding.editTextPassword.text.toString()
                if (ssid.isNotBlank()) {
                    sendData("{\"command\": \"setWiFi\", \"ssid\": \"$ssid\", \"pass\": \"$password\"}")
                } else {
                    showToast("Заполните SSID")
                }
            } else {
                showToast("Сначала выберите устройство")
            }
        }
    }
}
