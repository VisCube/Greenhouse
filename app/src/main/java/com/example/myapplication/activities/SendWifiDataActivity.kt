package com.example.myapplication.activities

import android.os.Bundle
import com.example.myapplication.databinding.ActivitySendWifiDataBinding
import java.util.UUID

class SendWifiDataActivity : BaseBleActivity() {

    override val serviceUUID: UUID = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb")
    override val characteristicUUID: UUID = UUID.fromString("00002A57-0000-1000-8000-00805f9b34fb")

    private lateinit var binding: ActivitySendWifiDataBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendWifiDataBinding.inflate(layoutInflater)
        setContentView(binding.root)
        reconnectToDevice()

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
