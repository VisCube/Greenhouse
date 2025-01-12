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
    private val scanHandler = Handler(Looper.getMainLooper())
    private var awaitResponse = false
    protected var isDeviceReady = false

    protected abstract val serviceUUID: UUID
    protected abstract val characteristicUUID: UUID
    protected abstract fun processCharacteristicValue(value: ByteArray)
    protected open fun onDeviceReady() {    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        requestPermissionsIfNeeded()
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )

            else -> listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val neededPermissions = permissions.filterNot {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (neededPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(neededPermissions.toTypedArray())
        } else {
            checkBluetooth()
        }
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.values.any { !it }) {
                showToast("Необходимо предоставить разрешения для работы с Bluetooth")
            } else {
                checkBluetooth()
            }
        }

    protected fun checkBluetooth(): Boolean {
        val bluetoothAdapter = bluetoothManager.adapter
        return if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            showToast("Включите Bluetooth")
            false
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    protected fun startScan() {
        if (!checkBluetooth()) return
        awaitResponse = false
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

    @SuppressLint("MissingPermission")
    protected fun saveDeviceToPreferences(device: BluetoothDevice) {
        val sharedPreferences = getSharedPreferences("BLE_PREFS", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString("SAVED_DEVICE", device.address)
            .apply()
    }

    @SuppressLint("MissingPermission")
    protected open fun connectToDevice() {
        bluetoothGatt =
            selectedDevice?.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
            } else {
                isDeviceReady = false
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                isDeviceReady = true
                runOnUiThread {
                    onDeviceReady()
                }
            }
        }


        @SuppressLint("MissingPermission")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                processCharacteristicValue(characteristic.value)
            } else {
                showToast("Ошибка при чтении характеристики")
            }
            awaitResponse = false
        }
    }

    @SuppressLint("MissingPermission")
    protected fun sendData(data: String, requiresResponse: Boolean = false) {
        if (!isDeviceReady ) {
            return
        }
        val service = bluetoothGatt?.getService(serviceUUID)
        val characteristic = service?.getCharacteristic(characteristicUUID)
        if (characteristic != null) {
            characteristic.value = data.toByteArray()
            val success = bluetoothGatt?.writeCharacteristic(characteristic) ?: false
            if (success) {
                if (requiresResponse) {
                    awaitResponse = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        bluetoothGatt?.readCharacteristic(characteristic)
                    }, 500)
                }
            } else {
                showToast("Ошибка при отправке команды")
            }
        } else {
            showToast("Характеристика не найдена")
        }
    }


    @SuppressLint("MissingPermission")
    protected fun disconnectGatt() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    protected fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        disconnectGatt()
    }

    @SuppressLint("MissingPermission")
    protected fun resetConnection() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        Thread.sleep(1000)
        connectToDevice()
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

    protected fun deleteDevice() {
        val sharedPreferences = getSharedPreferences("BLE_PREFS", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .remove("SAVED_DEVICE")
            .apply()
        disconnectGatt()
        selectedDevice = null;
    }
}
