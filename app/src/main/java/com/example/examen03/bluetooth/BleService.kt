package com.example.examen03.bluetooth
import android.Manifest
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import com.example.examen03.data.model.Contact
import com.example.examen03.data.model.HealthStatus
import com.example.examen03.data.repository.AppRepository
import com.example.examen03.network.SupabaseService
import java.util.*
import android.util.Log
import androidx.core.app.ActivityCompat


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
        Log.d("BLE_SERVICE", "Servicio iniciado con userId: $userId")

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

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
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
        Log.d("BLE_SERVICE", "Iniciando escaneo BLE...")

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val rssi = result.rssi

                Log.d("BLE_SERVICE", "Dispositivo detectado con RSSI: $rssi")

                if (rssi > RSSI_THRESHOLD) {
                    result.scanRecord?.serviceData?.get(ParcelUuid(SERVICE_UUID))?.let { data ->
                        val detectedUserId = String(data)
                        Log.d("BLE_SERVICE", "Dispositivo cercano detectado con ID: $detectedUserId")

                        handleContactDetected(detectedUserId, rssi)
                    }?: Log.w("BLE_SERVICE", "No se encontró serviceData en el scan result")
                }else {
                    Log.d("BLE_SERVICE", "Dispositivo ignorado por RSSI bajo: $rssi")
                }
            }
            override fun onScanFailed(errorCode: Int) {
                Log.e("BLE_SERVICE", "❌ Error al iniciar escaneo BLE. Código de error: $errorCode")
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BLE_SERVICE", "❌ Permiso BLUETOOTH_SCAN denegado. No se puede iniciar escaneo.")

            return
        }
        bleScanner.startScan(listOf(scanFilter), scanSettings, scanCallback)
        Log.d("BLE_SERVICE", "Escaneo BLE iniciado correctamente.")

    }


    // ========== FLUJO COMPLETO DE ACTUALIZACIÓN A SUPABASE ==========



    // 2. ACTUALIZACIÓN DEL USUARIO ACTUAL A INFECTED
    private fun updateCurrentUserHealthStatusToInfected() {
        // LLAMADA DIRECTA A SUPABASE para actualizar usuario actual
        repository.updateHealthStatus(userId!!, HealthStatus.INFECTED,
            object : SupabaseService.Callback<Boolean> {
                override fun onSuccess(result: Boolean) {
                    Log.d("BLE_SERVICE", "✅ SUPABASE UPDATED: Current user → INFECTED")
                }
                override fun onError(error: String) {
                    Log.e("BLE_SERVICE", "❌ SUPABASE ERROR: Failed to update current user to INFECTED: $error")
                }
            })
    }


    // 3. ACTUALIZACIÓN DEL USUARIO ACTUAL A AT_RISK
    private fun updateCurrentUserHealthStatus() {
        // LLAMADA DIRECTA A SUPABASE para actualizar usuario actual
        repository.updateHealthStatus(userId!!, HealthStatus.AT_RISK,
            object : SupabaseService.Callback<Boolean> {
                override fun onSuccess(result: Boolean) {
                    Log.d(TAG, "✅ SUPABASE UPDATED: Current user → AT_RISK")
                }
                override fun onError(error: String) {
                    Log.e(TAG, "❌ SUPABASE ERROR: Failed to update current user to AT_RISK: $error")
                }
            })
    }

    // 4. ACTUALIZACIÓN DEL CONTACTO DETECTADO A AT_RISK
    private fun updateContactHealthStatusToAtRisk(contactUserId: String) {
        // LLAMADA DIRECTA A SUPABASE para actualizar contacto detectado
        repository.updateHealthStatus(contactUserId, HealthStatus.AT_RISK,
            object : SupabaseService.Callback<Boolean> {
                override fun onSuccess(result: Boolean) {
                    Log.d(TAG, "✅ SUPABASE UPDATED: Contact $contactUserId → AT_RISK")
                }
                override fun onError(error: String) {
                    Log.e(TAG, "❌ SUPABASE ERROR: Failed to update contact to AT_RISK: $error")
                }
            })
    }

// ========== MÉTODO DEL REPOSITORIO QUE HACE EL UPDATE A SUPABASE ==========
// (Este código está en SupabaseService.kt)



// ========== EJEMPLO DE LOS UPDATES QUE SE HACEN ==========
    /*
    ESCENARIO: Usuario AT_RISK detecta a usuario HEALTHY

    1. UPDATE en Supabase:
       PATCH /rest/v1/users?id=eq.USER_ACTUAL_ID
       Body: {"health_status": "INFECTED"}

    2. UPDATE en Supabase:
       PATCH /rest/v1/users?id=eq.CONTACTO_ID
       Body: {"health_status": "AT_RISK"}

    Resultado en base de datos:
    - Usuario actual: HEALTHY → INFECTED ✅
    - Contacto detectado: HEALTHY → AT_RISK ✅
    */

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
                updateCurrentUserHealthStatusToInfected() // → UPDATE a Supabase
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
                    if (it.healthStatus == HealthStatus.AT_RISK) {
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
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            bleScanner.stopScan(it)
        }
    }
}