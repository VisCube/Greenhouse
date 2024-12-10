package com.example.myapplication

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.myapplication.databinding.ActivitySendWifiDataBinding
import java.io.IOException
import java.util.UUID

class SendWifiDataActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendWifiDataBinding
    private lateinit var bluetoothManager: BluetoothManager
    private var selectedDevice: BluetoothDevice? = null
    private var bluetoothSocket: BluetoothSocket? = null

    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendWifiDataBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        requestPermissions()

        binding.btnScanDevices.setOnClickListener {
            scanBluetoothDevices()
        }

        binding.btnSendWifiData.setOnClickListener {
            val ssid = binding.editTextSsid.text.toString()
            val password = binding.editTextPassword.text.toString()
            if (ssid.isNotBlank()) {
                sendWifiData(ssid, password)
            } else {
                Toast.makeText(this, "Заполните SSID", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    1
                )
            }
        }
    }

    private fun scanBluetoothDevices() {
        val bluetoothAdapter = bluetoothManager.adapter
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Включите Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        val pairedDevices = bluetoothAdapter.bondedDevices
        if (pairedDevices.isNotEmpty()) {
            val deviceNames = pairedDevices.map { it.name ?: "Неизвестное устройство" }
            val deviceArray = pairedDevices.toTypedArray()

            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("Выберите устройство Bluetooth")
            builder.setItems(deviceNames.toTypedArray()) { _, which ->
                selectedDevice = deviceArray[which]
                binding.deviceChosen.text = "Выбрано устройство: ${selectedDevice?.name}";
            }
            builder.show()
        } else {
            Toast.makeText(this, "Связанных устройств не найдено", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendWifiData(ssid: String, password: String) {
        if (selectedDevice == null) {
            Toast.makeText(this, "Сначала выберите устройство Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            bluetoothSocket = selectedDevice?.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket?.connect()

            val outputStream = bluetoothSocket?.outputStream
            if (outputStream != null) {
                val data = if (password.isNotBlank()) {
                    "SSID:$ssid;PASSWORD:$password"
                } else {
                    "SSID:$ssid;PASSWORD:"
                }
                outputStream.write(data.toByteArray())
                Toast.makeText(this, "Данные Wi-Fi успешно отправлены", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Не удалось получить поток вывода", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка отправки данных: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            try {
                bluetoothSocket?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
