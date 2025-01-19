package com.webaddicted.imagepickercompressor.utils.common
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager
import android.location.Location;
import android.location.LocationManager
import android.os.Build
import android.os.Looper;
import android.util.Log
import androidx.core.app.ActivityCompat

import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import java.util.concurrent.TimeUnit;
class LocationLiveData(private val context: Context) : MutableLiveData<LiveDataWrapper<Location>>() {

    companion object {
        private val INTERVAL = TimeUnit.SECONDS.toMillis(60) // 1 min
        private val FASTEST_INTERVAL = TimeUnit.SECONDS.toMillis(30) // 30 seconds
        private const val LOCATION_REQUEST_CODE = 6000
    }

    private val fusedLocationClient: FusedLocationProviderClient? by lazy {
        if (isGooglePlayServicesAvailable() == ConnectionResult.SUCCESS && hasGPSDevice()) {
            LocationServices.getFusedLocationProviderClient(context)
        } else null
    }

    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        interval = INTERVAL
        fastestInterval = FASTEST_INTERVAL
    }

    private var locationCallback: LocationCallback? = null

    init {
        value = LiveDataWrapper.loading(null)
        initLocationSettings()
    }

    private fun initLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.firstOrNull()?.let { location ->
                    value = LiveDataWrapper.success(location)
                    stopLocationUpdates()
                }
            }
        }
    }

    private fun initLocationSettings() {
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .build()

        val settingsClient = LocationServices.getSettingsClient(context)
        settingsClient.checkLocationSettings(settingsRequest)
            .addOnFailureListener { exception ->
                handleLocationSettingsFailure(exception)
            }
    }

    private fun handleLocationSettingsFailure(exception: Exception) {
        value = LiveDataWrapper.error(null)
        if (exception is ResolvableApiException) {
            try {
                exception.startResolutionForResult(context as Activity, LOCATION_REQUEST_CODE)
            } catch (e: IntentSender.SendIntentException) {
                Log.e("LocationLiveData", "Resolution failed: ${e.message}")
            }
        }
    }

    private fun isPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onActive() {
        if (isPermissionGranted()) {
            initLocationCallback()
            fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
        } else {
            Log.w("LocationLiveData", "Location permission not granted.")
        }
    }

    override fun onInactive() {
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    private fun isGooglePlayServicesAvailable(): Int {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
    }

    private fun hasGPSDevice(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager?.allProviders?.contains(LocationManager.GPS_PROVIDER) == true
    }
}