package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import com.example.myapplication.databinding.ActivityStartBinding
import java.util.UUID

class StartActivity() : BaseBleActivity() {
    private lateinit var binding: ActivityStartBinding

    override val serviceUUID: UUID = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb")
    override val characteristicUUID: UUID = UUID.fromString("00002A57-0000-1000-8000-00805f9b34fb")

    override fun processCharacteristicValue(value: ByteArray) {
    }

    private var deviceSelectionDialogShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        deleteDevice()
        startScan()
    }

    override fun stopScan() {
        if (!scanning) return
        super.stopScan()
        if (bleDevices.isNotEmpty() && !deviceSelectionDialogShown) {
            showDeviceSelectionDialog()
        }

    }

    @SuppressLint("MissingPermission")
    override fun showDeviceSelectionDialog() {
        deviceSelectionDialogShown = true // Устанавливаем флаг
        val deviceNames = bleDevices.map { it.name ?: "Неизвестное устройство" }
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Выберите устройство BLE")
        builder.setItems(deviceNames.toTypedArray()) { _, which ->
            selectedDevice = bleDevices[which]
            saveDeviceToPreferences(selectedDevice!!)
            moveToMainActivity()
        }
        builder.show()
    }


    private fun moveToMainActivity() {
        val intent = Intent(this, GetDataByBLEActivity::class.java)
        startActivity(intent)
        finish()
    }
}