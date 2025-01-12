package com.example.myapplication.activities

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
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
import java.util.UUID

abstract class BaseBleActivity : AppCompatActivity() {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    protected var selectedDevice: BluetoothDevice? = null
    protected var bluetoothGatt: BluetoothGatt? = null
    protected val bleDevices = mutableListOf<BluetoothDevice>()
    protected var scanning = false
    private var awaitingResponse = false
    private val scanHandler = Handler(Looper.getMainLooper())

    protected abstract val serviceUUID: UUID
    protected abstract val characteristicUUID: UUID

    protected open fun processCharacteristicValue(value: ByteArray) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        requestPermissionsIfNeeded()
    }

    private fun requestPermissionsIfNeeded() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            checkBluetooth()
        }
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val deniedPermissions = permissions.filter { !it.value }.keys
            if (deniedPermissions.isNotEmpty()) {
                showToast("Необходимо предоставить разрешения для работы с Bluetooth и расположением.")
            } else {
                checkBluetooth()
            }
        }


    protected fun checkBluetooth(): Boolean {
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            showToast("Включите Bluetooth")
            return false
        }
        return true
    }

    @SuppressLint("MissingPermission")
    protected fun startScan() {
        if (!checkBluetooth()) return

        bluetoothLeScanner = bluetoothManager.adapter.bluetoothLeScanner
        showToast("Начинаем поиск BLE-устройств...")
        bleDevices.clear()
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        bluetoothLeScanner.startScan(null, scanSettings, scanCallback)
        scanning = true

        scanHandler.postDelayed({ stopScan() }, 1_500)
    }

    @SuppressLint("MissingPermission")
    protected open fun stopScan() {
        if (scanning) {
            bluetoothLeScanner.stopScan(scanCallback)
            scanning = false
            showToast("Сканирование завершено")
            showDeviceSelectionDialog()
        }
    }

    @SuppressLint("MissingPermission")
    protected open fun showDeviceSelectionDialog() {
        if (bleDevices.isEmpty()) {
            showToast("BLE-устройства не найдены")
            return
        }

        val deviceNames = bleDevices.map { it.name ?: "Неизвестное устройство" }
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Выберите устройство BLE")
        builder.setItems(deviceNames.toTypedArray()) { _, which ->
            selectedDevice = bleDevices[which]
            saveDeviceToPreferences(selectedDevice!!)
            connectToDevice()
        }
        builder.show()
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (device.name != null && !bleDevices.contains(device)) {
                bleDevices.add(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            showToast("Ошибка сканирования BLE: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    protected open fun connectToDevice() {
        bluetoothGatt =
            selectedDevice?.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                } else {
                    gatt.close()
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && awaitingResponse) {
                readCharacteristic()
            }
        }


        @SuppressLint("MissingPermission")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val value = characteristic.value

                // Проверяем, нужно ли читать этот ответ
                if (awaitingResponse) {
                    awaitingResponse = false // Сбрасываем флаг
                    processCharacteristicValue(value)
                    resetConnection() // Обрываем соединение
                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun readCharacteristic() {
        val service = bluetoothGatt?.getService(serviceUUID)
        if (service == null) {
            showToast("Сервис BLE не найден")
            return
        }
        val characteristic = service.getCharacteristic(characteristicUUID)
        if (characteristic != null) {
            bluetoothGatt?.readCharacteristic(characteristic)
        } else {
            showToast("Характеристика не найдена")
        }
    }

    @SuppressLint("MissingPermission")
    protected fun sendData(dataToSend: String, awaitResponse: Boolean = false) {
        val service = bluetoothGatt?.getService(serviceUUID)
        if (service == null) {
            showToast("Сервис BLE не найден")
            return
        }
        val characteristic = service.getCharacteristic(characteristicUUID)
        if (characteristic != null) {
            awaitingResponse = awaitResponse // Устанавливаем флаг ожидания ответа
            val dataBytes = dataToSend.toByteArray()
            characteristic.setValue(dataBytes)
            val success = bluetoothGatt?.writeCharacteristic(characteristic) ?: false
            if (!success) {
                showToast("Ошибка при отправке данных")
                awaitingResponse = false // Сбрасываем флаг при ошибке
            }
        } else {
            showToast("Характеристика не найдена")
        }
    }


    protected fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    @SuppressLint("MissingPermission")
    protected fun reconnectToDevice() {
        val sharedPreferences = getSharedPreferences("BLE_PREFS", Context.MODE_PRIVATE)
        val savedDeviceAddress = sharedPreferences.getString("SAVED_DEVICE", null)

        if (savedDeviceAddress != null) {
            val bluetoothAdapter = bluetoothManager.adapter
            val device = bluetoothAdapter.getRemoteDevice(savedDeviceAddress)
            selectedDevice = device
            connectToDevice()
        }
    }

    @SuppressLint("MissingPermission")
    protected fun deleteDevice() {
        val sharedPreferences = getSharedPreferences("BLE_PREFS", Context.MODE_PRIVATE)
        sharedPreferences.edit().remove("SAVED_DEVICE").apply()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        selectedDevice = null
        Thread.sleep(100)
    }


    @SuppressLint("MissingPermission")
    protected fun saveDeviceToPreferences(device: BluetoothDevice) {
        val sharedPreferences = getSharedPreferences("BLE_PREFS", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString("SAVED_DEVICE", device.address)
            .apply()
    }

    @SuppressLint("MissingPermission")
    protected fun resetConnection() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        Thread.sleep(50)
        connectToDevice()
    }


}

