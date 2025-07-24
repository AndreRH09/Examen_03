package com.example.examen03.bluetooth
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import com.example.examen03.data.model.Contact
import com.example.examen03.data.model.HealthStatus
import com.example.examen03.data.repository.AppRepository
import com.example.examen03.network.SupabaseService
import java.util.*

class BleService : Service() {
    companion object {
        private val SERVICE_UUID = UUID.fromString("00001234-0000-1000-8000-00805f9b34fb")
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "contact_tracing_channel"
        private const val RSSI_THRESHOLD = -70
    }

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bleScanner: BluetoothLeScanner
    private lateinit var bleAdvertiser: BluetoothLeAdvertiser
    private lateinit var repository: AppRepository

    private var userId: String? = null
    private var scanCallback: ScanCallback? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bleScanner = bluetoothAdapter.bluetoothLeScanner
        bleAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        repository = AppRepository()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        userId = intent?.getStringExtra("USER_ID")
        if (userId == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())
        startBleOperations()

        return START_STICKY
    }

    private fun startBleOperations() {
        startAdvertising()
        startScanning()
    }

    private fun startAdvertising() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setConnectable(false)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .addServiceData(ParcelUuid(SERVICE_UUID), userId!!.toByteArray())
            .build()

        bleAdvertiser.startAdvertising(settings, data, object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                // Advertising iniciado
            }

            override fun onStartFailure(errorCode: Int) {
                // Error al iniciar advertising
            }
        })
    }

    private fun startScanning() {
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val rssi = result.rssi
                if (rssi > RSSI_THRESHOLD) {
                    result.scanRecord?.serviceData?.get(ParcelUuid(SERVICE_UUID))?.let { data ->
                        val detectedUserId = String(data)
                        handleContactDetected(detectedUserId, rssi)
                    }
                }
            }
        }

        bleScanner.startScan(listOf(scanFilter), scanSettings, scanCallback)
    }

    private fun handleContactDetected(detectedUserId: String, rssi: Int) {
        val contact = Contact(
            id = "${userId}_${detectedUserId}_${System.currentTimeMillis()}",
            userId = userId!!,
            contactUserId = detectedUserId,
            timestamp = System.currentTimeMillis(),
            rssi = rssi
        )

        // Guardar contacto en Supabase
        repository.saveContact(contact, object : SupabaseService.Callback<Boolean> {
            override fun onSuccess(result: Boolean) {
                // Contacto guardado
                checkHealthRisk(detectedUserId)
            }

            override fun onError(error: String) {
                // Error al guardar
            }
        })
    }

    private fun checkHealthRisk(contactUserId: String) {
        // Obtener información del contacto
        repository.getUserById(contactUserId, object : SupabaseService.Callback<com.example.examen03.data.model.User?> {
            override fun onSuccess(contactUser: com.example.examen03.data.model.User?) {
                contactUser?.let {
                    if (it.healthStatus == HealthStatus.INFECTED) {
                        // Actualizar estado del usuario actual a AT_RISK
                        updateCurrentUserHealthStatus()
                    }
                }
            }

            override fun onError(error: String) {
                // Error al obtener usuario
            }
        })
    }

    private fun updateCurrentUserHealthStatus() {
        repository.updateHealthStatus(userId!!, HealthStatus.AT_RISK,
            object : SupabaseService.Callback<Boolean> {
                override fun onSuccess(result: Boolean) {
                    // Estado actualizado
                }

                override fun onError(error: String) {
                    // Error al actualizar
                }
            })
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Contact Tracing",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }.apply {
            setContentTitle("Rastreo de Contactos Activo")
            setContentText("Detectando dispositivos cercanos")
            setSmallIcon(android.R.drawable.ic_dialog_info)
        }.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        scanCallback?.let {
            bleScanner.stopScan(it)
        }
    }
}