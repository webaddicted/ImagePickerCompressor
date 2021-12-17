package com.webaddicted.imagepickercompressor.utils.common

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.webaddicted.imagepickercompressor.R
import java.io.ByteArrayOutputStream

object GlobalUtils {
    var toast: Toast? = null
    fun Context.showToast(message: String?) {
        toast?.cancel()
        toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
        toast?.show()
    }

    fun hideKeyboardFrom(context: Context, view: View?) {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    fun setBackgroundTintColor(view: TextView, color: Int) {
        view.backgroundTintList = ContextCompat.getColorStateList(view.context, color)
    }

    fun logPrint(tag: String? = "TAG", msg: String? = "") {
        msg?.let { Log.d(tag, it) }
    }

    fun uriToBitmap(mContext: Context, uri: Uri): Bitmap {
        return BitmapFactory.decodeStream(mContext.contentResolver.openInputStream(uri))
    }

    fun encodeImage(bm: Bitmap?, quality: Int, mContext: Context): String {
        try {
            bm?.let {
                val baos = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                val b = baos.toByteArray()
                return Base64.encodeToString(b, Base64.DEFAULT)
            }
        } catch (e: OutOfMemoryError) {
            mContext.showToast(mContext.getString((R.string.insufficient_memory_error)))
        } catch (e: Exception) {
            mContext.showToast(mContext.getString((R.string.something_went_wrong)))
        }
        return ""
    }

    fun bitmapToString(imageBitmap: Bitmap?): String {
        val baos = ByteArrayOutputStream()
        imageBitmap?.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val imageBytes: ByteArray = baos.toByteArray()
        val encodedImage: String = Base64.encodeToString(imageBytes, Base64.DEFAULT)
        return encodedImage
    }

    fun delay(secDelay: Int = 1, function: () -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed({
            function()
        }, secDelay * 1000L)
    }

    fun versionCompare(v1: String?, v2: String?): Boolean {
        val currVersion = v2?.split('.')
        val minPrefVersion = v1?.split('.')

        var i = 0
        when {
            currVersion!![i].toInt() < minPrefVersion!![i].toInt() -> {
                return false
            }
            currVersion[i].toInt() > minPrefVersion[i].toInt() -> {
                return true
            }
            else -> {
                i++
                when {
                    currVersion[i].toInt() > minPrefVersion[i].toInt() -> {
                        return true
                    }
                    currVersion[i].toInt() < minPrefVersion[i].toInt() -> {
                        return false
                    }
                    else -> {
                        i++
                        return when {
                            currVersion[i].toInt() > minPrefVersion[i].toInt() -> {
                                true
                            }
                            currVersion[i].toInt() < minPrefVersion[i].toInt() -> {
                                false
                            }
                            currVersion[i].toInt() == minPrefVersion[i].toInt() -> {
                                true
                            }
                            else -> {
                                false
                            }
                        }
                    }
                }
            }
        }

    }

    fun setUserInteraction(isEnable: Boolean, window: Window?) {
        if (isEnable) {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        } else {
            window?.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
        }
    }

}