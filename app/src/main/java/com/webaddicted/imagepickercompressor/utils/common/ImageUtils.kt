package com.webaddicted.imagepickercompressor.utils.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Base64
import android.util.Log
import com.webaddicted.imagepickercompressor.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ImageUtils {

    private var mExceptionRetryCounter = 0
    const val NEW_CAMERA_SAMPLING = 2
    const val NEW_CAMERA_JPEG_QUALITY = 90

    fun compressInternal(
        imagePath: String,
        samplingSize: Int,
        imageQuality: Int,
        size: Int = 1024,
        config: Bitmap.Config
    ): String {
        val imageCompressionSize = size * 1024
        var base64 = ""
        var bitmap:Bitmap? = null
        mExceptionRetryCounter = 0
        var imageCompressCounter = 0
        val file = File(imagePath)
        while (file.length() > imageCompressionSize) {
            imageCompressCounter++
            try {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = false
                options.inSampleSize = samplingSize
                options.inPreferredConfig = config
                bitmap = BitmapFactory.decodeFile(imagePath, options)
                bitmap=fixImageRotation(bitmap,file)
                if(BuildConfig.DEBUG) Log.d("Base64Image","Without Compress Image COMPRESSION Type - 2 : ${getBitmapSize(file)}")
                val out = FileOutputStream(imagePath)
                bitmap.compress(Bitmap.CompressFormat.JPEG, imageQuality, out)
                out.flush()
                out.close()
            } catch (e: Exception) {
                if (mExceptionRetryCounter == 3) {
                    break
                }
                mExceptionRetryCounter++
            }
        }
        if (bitmap!=null){
            val stream = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, imageQuality, stream, )
            val array = stream.toByteArray()
            base64 = Base64.encodeToString(array, Base64.NO_WRAP)
            stream.flush()
            stream.close()
        }else{
            val bytes = ByteArray(file.length().toInt())
            val inputStream = FileInputStream(file)
            inputStream.read(bytes)
            inputStream.close()
            base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
        return base64
    }



    fun fixImageRotation(bitmap: Bitmap, mFile:File) : Bitmap {
        var bitmape=bitmap
        val exif = ExifInterface(mFile.absolutePath)
        if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equals("6")) {
            bitmape = rotateImage(bitmap, 90)
        } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equals("8")) {
            bitmape = rotateImage(bitmap, 270)
        } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equals("3")) {
            bitmape = rotateImage(bitmap, 180)
        }
        //        else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equals("0")) {
        //            bitmape = RotateBitmap(bitmap, 90f)
        //        }
        return bitmape
    }

    fun rotateImage(img: Bitmap, degree: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }
    fun getBitmapSize(file: File): String {
        // Get the byte count of the Bitmap
        val byteCount = file.length()
        // Convert bytes to kilobytes
        val KB = byteCount/1024
        val MB = KB/1024

        return " : byte-$byteCount\nkb-$KB\nmb-$MB"
    }
    fun getBitmapSize(bitmap: Bitmap): String {
        // Get the byte count of the Bitmap
        val byteCount = bitmap.byteCount
        // Convert bytes to kilobytes
        val KB = byteCount/1024
        val MB = KB/1024

        return " : byte-$byteCount\nkb-$KB\nmb-$MB"
    }
    fun getBase64ImageSize(base64Image: String): String {
        // Decode Base64 string into a byte array
        val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
        // Get the length of the byte array
        val byte = decodedBytes.size
        val KB = byte/1024
        val MB = KB/1024

        return " byte-$byte\nkb-$KB\nmb-$MB"
    }
}