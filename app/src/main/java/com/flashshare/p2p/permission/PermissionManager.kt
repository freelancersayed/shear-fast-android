package com.flashshare.p2p.permission

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * PermissionManager computes and verifies dynamic runtime permissions and
 * hardware states (Wi-Fi, Bluetooth, Location) required for FlashShare P2P
 * across all Android API levels (Android 8 to Android 15+).
 */
object PermissionManager {

    /**
     * Resolves the exact list of runtime permissions needed based on Build.VERSION.SDK_INT.
     */
    fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+ (Android 13, 14, 15)
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31, 32 (Android 12)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else { // Android 8 to 11
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        return permissions.toTypedArray()
    }

    /**
     * Checks if all mandatory permissions are currently granted by user.
     */
    fun hasAllPermissions(context: Context): Boolean {
        return getMissingPermissions(context).isEmpty()
    }

    /**
     * Returns the sub-list of permissions that are still not granted.
     */
    fun getMissingPermissions(context: Context): List<String> {
        return getRequiredPermissions().filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks if Wi-Fi is enabled on the device.
     */
    fun isWifiEnabled(context: Context): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        return wifiManager?.isWifiEnabled == true
    }

    /**
     * Checks if Bluetooth is enabled on the device.
     */
    @Suppress("DEPRECATION")
    fun isBluetoothEnabled(): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        return adapter != null && adapter.isEnabled
    }

    /**
     * Checks if Location Service (GPS) is turned ON.
     * Crucial for Wi-Fi P2P and Bluetooth device discovery on Android.
     */
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Returns a human-friendly list of hardware services that need to be turned on.
     */
    fun getHardwareWarnings(context: Context): List<String> {
        val warnings = mutableListOf<String>()
        if (!isWifiEnabled(context)) {
            warnings.add("Wi-Fi is turned off")
        }
        if (!isBluetoothEnabled()) {
            warnings.add("Bluetooth is turned off")
        }
        if (!isLocationEnabled(context)) {
            warnings.add("Location/GPS is turned off")
        }
        return warnings
    }
}
