package com.webaddicted.imagepickercompressor.utils.common
import android.Manifest
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.os.Looper;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gms.common.api.Response
import java.util.concurrent.TimeUnit;

class LiveDataWrapper<T>(
    val response: Response<*>? = null,
    val status: Status,
    val data: T? = null,
    val msg: String? = null,
    val throwable: Throwable? = null
) {

    companion object {
        fun <T> success(response: Response<*>? = null, data: T? = null): LiveDataWrapper<T> {
            return LiveDataWrapper(response, Status.SUCCESS, data)
        }

        fun <T> success(data: T?): LiveDataWrapper<T> {
            return LiveDataWrapper(status = Status.SUCCESS, data = data)
        }

        // API response came but response.body is empty or null
        fun <T> error(response: Response<*>? = null): LiveDataWrapper<T> {
            return LiveDataWrapper(response, Status.ERROR)
        }

        fun <T> error(data: T?): LiveDataWrapper<T> {
            return LiveDataWrapper(status = Status.ERROR, data = data)
        }

        // Useful to show cached data from ROOM DB when API response returns null data
        fun <T> error(response: Response<*>?, data: T?): LiveDataWrapper<T> {
            return LiveDataWrapper(response, Status.ERROR, data)
        }

        // When API fails and onFailure() is called
        fun <T> failure(throwable: Throwable): LiveDataWrapper<T> {
            return LiveDataWrapper(status = Status.FAILURE, throwable = throwable)
        }

        // Useful to show cached data from ROOM DB when API failure occurs
        fun <T> failure(throwable: Throwable, data: T?): LiveDataWrapper<T> {
            return LiveDataWrapper(status = Status.FAILURE, data = data, throwable = throwable)
        }

        fun <T> loading(data: T?): LiveDataWrapper<T> {
            return LiveDataWrapper(status = Status.LOADING, data = data)
        }

        // Useful to show cached data from ROOM DB when the user is offline
        fun <T> offline(msg: String, data: T?): LiveDataWrapper<T> {
            return LiveDataWrapper(status = Status.OFFLINE, data = data, msg = msg)
        }
    }
}


enum class Status {
    SUCCESS,
    ERROR,
    LOADING,
    FAILURE,
    OFFLINE
}
