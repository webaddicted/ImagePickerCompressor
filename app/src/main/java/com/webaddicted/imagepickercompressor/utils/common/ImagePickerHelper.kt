package com.webaddicted.imagepickercompressor.utils.common

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.ACTION_IMAGE_CAPTURE
import android.util.Base64
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.webaddicted.imagepickercompressor.R
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImagePickerHelper {

    private lateinit var selectedImgUri: Uri
    private const val IMG_FILE_NAME_FORMAT = "yyyyMMdd_HHmmss"
    private const val IMG_FILE_EXT = "jpeg"
    private const val IMG_DIR = "app_imgs_dir"
    private var mCurrentPhotoPath: String = ""
    private lateinit var imageListener: (File, Bitmap) -> Unit

    enum class ImgPickerType(val value: Int) {
        OPEN_CAMERA(5002),
        SELECT_IMAGE(5003),
        CHOOSER_CAMERA_GALLERY(5004),
    }

    fun getImage(
        activity: AppCompatActivity,
        startForResult: ActivityResultLauncher<Intent>,
        pickerType: ImgPickerType,
        imagePickerListener: (File, Bitmap) -> Unit
    ) {
        imageListener = imagePickerListener

        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT <= 32) {
            permissions.addAll(
                listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }

        PermissionHelper.requestMultiplePermission(activity, permissions) { isGranted, _ ->
            if (isGranted) {
                openIntentChooser(activity, startForResult, pickerType)
            } else {
                DialogUtils.showDialog(
                    activity,
                    message = activity.getString(R.string.permission_denied)
                )
            }
        }
    }

    private fun openIntentChooser(
        activity: AppCompatActivity,
        startForResult: ActivityResultLauncher<Intent>,
        pickerType: ImgPickerType
    ) {
        try {
            val takePictureIntent = Intent(ACTION_IMAGE_CAPTURE)
            val photoFile = createImgFile(activity)
            selectedImgUri = FileProvider.getUriForFile(
                activity, "${activity.packageName}.provider", photoFile
            )
            takePictureIntent.apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                putExtra(MediaStore.EXTRA_OUTPUT, selectedImgUri)
            }

            when (pickerType) {
                ImgPickerType.OPEN_CAMERA -> {
                    startForResult.launch(takePictureIntent)
                }
                ImgPickerType.SELECT_IMAGE -> {
                    val selectImageIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "image/*"
                    }
                    startForResult.launch(Intent.createChooser(selectImageIntent, "Select Picture"))
                }
                ImgPickerType.CHOOSER_CAMERA_GALLERY -> {
                    val pickGalleryIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    val chooserIntent = Intent.createChooser(
                        pickGalleryIntent,
                        activity.getString(R.string.capture_photo)
                    ).apply {
                        putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(takePictureIntent))
                    }
                    startForResult.launch(chooserIntent)
                }
            }
        } catch (e: IOException) {
            handleError(activity, e)
        }
    }

    fun onActivityResult(activity: Activity, activityResult: ActivityResult) {
        val data = activityResult.data
        selectedImgUri = data?.data ?: selectedImgUri
        postProcessImage(activity, data)
    }

    private fun postProcessImage(activity: Activity, data: Intent?) {
        try {
            val bitmap = data?.extras?.getParcelable<Bitmap>("data")
                ?: getBitmapFromUri(activity, selectedImgUri)
            bitmap?.let {
                val file = saveBitmapToFile(activity, it)
                if (file != null) imageListener(file, it)
            }
        } catch (e: Exception) {
            handleError(activity, e)
        }
    }

    private fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun createImgFile(activity: Activity): File {
        val imgFileName =
            SimpleDateFormat(IMG_FILE_NAME_FORMAT, Locale.US).format(Date()) + ".$IMG_FILE_EXT"
        val folder = File(activity.filesDir, IMG_DIR).apply {
            if (!exists()) mkdir()
        }
        return File(folder, imgFileName).apply {
            createNewFile()
            mCurrentPhotoPath = absolutePath
            updateGallery(activity, mCurrentPhotoPath)
        }
    }

    private fun saveBitmapToFile(context: Context, bitmap: Bitmap): File? {
        return try {
            val imgFileName =
                SimpleDateFormat(IMG_FILE_NAME_FORMAT, Locale.US).format(Date()) + ".$IMG_FILE_EXT"
            val folder = File(context.filesDir, IMG_DIR).apply {
                if (!exists()) mkdir()
            }
            val file = File(folder, imgFileName)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun updateGallery(context: Context, imagePath: String) {
        MediaScannerConnection.scanFile(context, arrayOf(imagePath), null, null)
    }

    private fun handleError(activity: Activity, exception: Exception) {
        exception.printStackTrace()
        DialogUtils.showDialog(
            activity,
            message = activity.getString(R.string.something_went_wrong) + ": ${exception.message}"
        )
    }
    suspend fun convertBase64Image(imgString: String): String {
        val mFile = File(imgString)
        val imageCompressionType = 2L
        var base64: String = ""
        try {
            if (mFile.exists()) {
                if (mFile.path.contains(".pdf") || mFile.path.contains(".doc")) {
                    val size: Int = (mFile.length().toString() + "").toInt()
                    val bytes = ByteArray(size)
                    //                    Log.d("TestTag", "PDF Size : ${bytes.size}")
                    try {
                        val buf = BufferedInputStream(FileInputStream(mFile))
                        buf.read(bytes, 0, bytes.size)
                        buf.close()
                    } catch (e: FileNotFoundException) {
                        Log.d("convertBase64Image","FileNotFoundException $e")
                    } catch (e: IOException) {
                        Log.d("convertBase64Image","IOException $e")
                    }
                    base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                } else if(imageCompressionType == 2L) {
                    // new compression best compression technique
                    val imageCompressionType = 1024
                    base64 = ImageUtils.compressInternal(
                        imgString,
                        ImageUtils.NEW_CAMERA_SAMPLING,
                        ImageUtils.NEW_CAMERA_JPEG_QUALITY,
                        imageCompressionType,
                        Bitmap.Config.RGB_565)
                }else{
                    // without any compression
                    val bytes = ByteArray(mFile.length().toInt())
                    val inputStream = FileInputStream(mFile)
                    inputStream.read(bytes)
                    inputStream.close()
                    base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
            } else {
                //                LogUtility.d(TAG,getString(R.string.ihi_file_not_exist))
            }
        } catch (e: Exception) {
            Log.d("convertBase64Image","convertBase64Image $e")
        }
        return base64
    }
}
