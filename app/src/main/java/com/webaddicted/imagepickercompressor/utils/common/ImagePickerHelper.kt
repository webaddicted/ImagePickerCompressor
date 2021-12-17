package com.webaddicted.imagepickercompressor.utils.common

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.ACTION_IMAGE_CAPTURE
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.webaddicted.imagepickercompressor.R
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


object ImagePickerHelper {
    private lateinit var selectedImgUri: Uri
    private const val IMG_FILE_NAME_FORMAT = "yyyyMMdd_HHmmss"
    private const val IMG_FILE_EXT = "jpeg"
    private const val IMG_DIR = "app_imgs_dir"
    private var mCurrentPhotoPath: String = ""
    private lateinit var imageListener: (File, imageBitmap: Bitmap) -> Unit

    enum class ImgPickerType(val value: Int) {
        OPEN_CAMERA(5002),
        SELECT_IMAGE(5003),
        CHOOSER_CAMERA_GALLERY(5004),
    }

    fun getImage(
        activity: AppCompatActivity,
        startForResult: ActivityResultLauncher<Intent>,
        pickerType: ImgPickerType,
        imagePickerListener: (mFile: File, imageBitmap: Bitmap) -> Unit
    ) {
        imageListener = imagePickerListener
        val locationList = ArrayList<String>()
        locationList.add(Manifest.permission.CAMERA)
        locationList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        locationList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        PermissionHelper.requestMultiplePermission(activity, locationList)
        { isPermissionGranted: Boolean, _: List<String> ->
            if (isPermissionGranted) openIntentChooser(activity, startForResult, pickerType)
        }

    }

    private fun openIntentChooser(
        activity: AppCompatActivity,
        startForResult: ActivityResultLauncher<Intent>,
        pickerType: ImgPickerType
    ) {
        val takePicture = Intent(ACTION_IMAGE_CAPTURE)
        try {
            val photoFile = createImgFile(activity)
            selectedImgUri = FileProvider.getUriForFile(
                activity, activity.packageName + ".provider",
                photoFile
            )
            takePicture.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            takePicture.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            takePicture.putExtra(MediaStore.EXTRA_OUTPUT, selectedImgUri)
            when (pickerType) {
                ImgPickerType.OPEN_CAMERA -> {
                    val pickPhoto = Intent(ACTION_IMAGE_CAPTURE)
                    pickPhoto.putExtra(MediaStore.EXTRA_OUTPUT, arrayOf(takePicture))
                    startForResult.launch(pickPhoto)
                }
                ImgPickerType.SELECT_IMAGE -> {
                    val intent = Intent()
                    intent.type = "image/*"
                    intent.action = Intent.ACTION_GET_CONTENT
                    startForResult.launch(Intent.createChooser(intent, "Select Picture"))
                }
                ImgPickerType.CHOOSER_CAMERA_GALLERY -> {
                    val pickPhoto =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    val chooser =
                        Intent.createChooser(pickPhoto, activity.getString(R.string.capture_photo))
                    chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(takePicture))
                    startForResult.launch(chooser)
                }
            }

        } catch (e: IOException) {
            GlobalUtils.logPrint(msg = e.toString())
            DialogUtils.showDialog(
                activity,
                message = activity.getString(R.string.something_went_wrong) + e.toString()
            )
        }
    }

    fun onActivityResult(mActivity: Activity, activityResult: ActivityResult) {
        val data = activityResult.data
        if (data != null) {
            if (data.data != null)
                selectedImgUri = data.data as Uri
            postCropImg(mActivity, data)
        }
    }

    private fun postCropImg(mActivity: Activity, data: Intent?) {
        if (data != null) {
            val imageBitmap = if (data.extras == null) {
                getBitmapFromUri(mActivity, selectedImgUri)
            } else {
                data.extras?.let { it.getParcelable<Uri>("data") as Bitmap }
            }
            imageBitmap?.let { imageListener(getFile(mActivity, imageBitmap)!!, it) }
        }
    }

    private fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        var bitmap: Bitmap? = null
        uri.authority?.let {
            try {
                val content = context.contentResolver.openInputStream(uri)
                bitmap = BitmapFactory.decodeStream(content)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return bitmap
    }

    private fun createImgFile(mActivity: Activity): File {
        val imgFileName =
            "${SimpleDateFormat(IMG_FILE_NAME_FORMAT, Locale.US).format(Date())}.$IMG_FILE_EXT"
        val folder = File(mActivity.filesDir, IMG_DIR)
        folder.mkdir()
        val image = File(folder, imgFileName)
        image.createNewFile()
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.absolutePath
        updateGallery(mActivity, mCurrentPhotoPath)
        return image
    }

    private fun getFile(mContext: Context, bmp: Bitmap?): File? {
        val imgFileName = "${
            SimpleDateFormat(
                IMG_FILE_NAME_FORMAT,
                Locale.US
            ).format(Date())
        }.${IMG_FILE_EXT}"
        val folder = File(mContext.filesDir, IMG_DIR)
        folder.mkdir()
        val file = File(folder, imgFileName)
        file.createNewFile()

        val outStream: OutputStream?
        try {
            outStream = FileOutputStream(file)
            bmp?.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
            outStream.flush()
            outStream.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            return null
        }
        return file
    }

    private fun updateGallery(context: Context, imagePath: String) {
        val file = File(imagePath)
        MediaScannerConnection.scanFile(
            context, arrayOf(file.toString()),
            null, null
        )
    }
}