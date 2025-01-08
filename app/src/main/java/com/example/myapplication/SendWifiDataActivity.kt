package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myapplication.databinding.ActivitySendWifiDataBinding
import java.util.UUID


class SendWifiDataActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendWifiDataBinding
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private var selectedDevice: BluetoothDevice? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private val bleDevices = mutableListOf<BluetoothDevice>()
    private var scanning = false
    private val scanHandler = Handler(Looper.getMainLooper())

    //TODO написать UUID из итогового скетча ардуино
    private val serviceUUID: UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc")
    private val characteristicUUID: UUID = UUID.fromString("87654321-4321-4321-4321-cba987654321")

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val deniedPermissions = permissions.filter { !it.value }.keys
            if (deniedPermissions.isNotEmpty()) {
                showToast("Необходимо предоставить разрешения для работы с Bluetooth и расположением.")
            } else {
                checkBluetooth()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendWifiDataBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        requestPermissionsIfNeeded()
        binding.btnScanDevices.setOnClickListener {
            if (!scanning) {
                startScan()
            } else {
                stopScan()
            }
        }

        binding.btnSendWifiData.setOnClickListener {
            val ssid = binding.editTextSsid.text.toString()
            val password = binding.editTextPassword.text.toString()
            if (ssid.isNotBlank()) {
                sendWifiData("{\"command\": \"setWiFi\", \"ssid\": \"$ssid\", \"pass\": \"$password\"}")
            } else {
                showToast("Заполните SSID")
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissionsToRequest = mutableListOf<String>()

        // Проверка для API 31 и выше
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        // Для API 29 и 30 только разрешение на геолокацию необходимо
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            checkBluetooth()
        }
    }

    private fun checkBluetooth(): Boolean {
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            showToast("Включите Bluetooth для отправки данных")
            return false
        }
        return true;
    }

    //TODO добавить фильтр для ардуино
    @SuppressLint("MissingPermission")
    private fun startScan() {
        if (!checkBluetooth()) {
            return
        }
        bluetoothLeScanner = bluetoothManager.adapter.bluetoothLeScanner

        showToast("Начинаем поиск BLE-устройств...")
        bleDevices.clear()
        val scanFilters = listOf<ScanFilter>()
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback)
        scanning = true

        scanHandler.postDelayed({
            stopScan()
        }, 5_000)
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        if (scanning) {
            bluetoothLeScanner.stopScan(scanCallback)
            showToast("Сканирование завершено")
            scanning = false
            showDeviceSelectionDialog()
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (device.name != null && !bleDevices.contains(device)) {
                bleDevices.add(device)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onBatchScanResults(results: List<ScanResult>) {
            for (result in results) {
                val device = result.device
                if (device.name != null && !bleDevices.contains(device)) {
                    bleDevices.add(device)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            showToast("Ошибка сканирования BLE: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private fun showDeviceSelectionDialog() {
        if (bleDevices.isEmpty()) {
            showToast("BLE-устройства не найдены")
            return
        }

        val deviceNames = bleDevices.map { it.name ?: "Неизвестное устройство" }
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Выберите устройство BLE")
        builder.setItems(deviceNames.toTypedArray()) { _, which ->
            selectedDevice = bleDevices[which]
            binding.deviceChosen.text = "Выбрано устройство: ${selectedDevice?.name}"
            connectToDevice()
        }
        builder.show()
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice() {
        bluetoothGatt = selectedDevice?.connectGatt(this, false, gattCallback, TRANSPORT_LE)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    gatt.close()
                }
            } else {
                gatt.close()
            }
        }
    }

    //Использую Deprecated методы т.к. для нового нужен более свежий min API
    @SuppressLint("MissingPermission")
    private fun sendWifiData(data: String) {
        val service = bluetoothGatt?.getService(serviceUUID)
        if (service == null) {
            showToast("Сервис BLE не найден")
            return
        }
        val characteristic = service.getCharacteristic(characteristicUUID)

        if (characteristic != null) {
            val dataBytes = data.toByteArray()
            characteristic.setValue(dataBytes)
            val success = bluetoothGatt?.writeCharacteristic(characteristic) ?: false
            if (success) {
                showToast("Данные успешно отправлены")
            } else {
                showToast("Ошибка при отправке данных")
            }
        } else {
            showToast("Характеристика не найдена")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
}
