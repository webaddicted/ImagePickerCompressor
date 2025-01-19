package com.webaddicted.imagepickercompressor.view.imgpicker

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.RadioButton
import androidx.core.view.drawToBitmap
import androidx.databinding.ViewDataBinding
import com.webaddicted.imagepickercompressor.R
import com.webaddicted.imagepickercompressor.databinding.ActivityScopedStorageBinding
import com.webaddicted.imagepickercompressor.utils.common.DownloadFileFromURLTask
import com.webaddicted.imagepickercompressor.utils.common.DownloadListener
import com.webaddicted.imagepickercompressor.utils.common.GlobalUtils.showToast
import com.webaddicted.imagepickercompressor.utils.common.GlobalUtils.toast
import com.webaddicted.imagepickercompressor.utils.constant.AppConstant
import com.webaddicted.imagepickercompressor.view.base.BaseActivity
import com.webaddicted.imagepickercompressor.view.splash.SplashActivity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


class ScopedStorageActivity : BaseActivity(R.layout.activity_scoped_storage) {

    //    private var outputPath: String = ""
    private var storageType: AppConstant.StorageType = AppConstant.StorageType.EXTERNAL_STORAGE
    private lateinit var mBinding: ActivityScopedStorageBinding
    private var filePath: File? = null

    companion object {
        val TAG = SplashActivity::class.qualifiedName
        fun newIntent(activity: Activity) {
            activity.startActivity(Intent(activity, ScopedStorageActivity::class.java))
        }

        fun newClearLogin(context: Activity?) {
            val intent = Intent(context, ScopedStorageActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            context?.startActivity(intent)
            context?.finish()
        }
    }

    override fun onBindTo(binding: ViewDataBinding) {
        mBinding = binding as ActivityScopedStorageBinding
        clickListener()
    }

    private fun clickListener() {
        mBinding.btnCreateFolder.setOnClickListener(this)
        mBinding.btnSaveImage.setOnClickListener(this)
        mBinding.rg.setOnCheckedChangeListener { group, checkedId ->
            Log.d("chk", "id$checkedId")
            val rb: RadioButton = group.findViewById(checkedId)
            when (checkedId) {
                R.id.rb_external_storage -> {
                    filePath = null
                    storageType = AppConstant.StorageType.EXTERNAL_STORAGE
                }
                R.id.rb_internal_download -> {
                    filePath = File("")
                    storageType = AppConstant.StorageType.INTERNAL_DOWNLOAD
                }
                R.id.rb_internal_dicm -> {
                    filePath = File("")
                    storageType = AppConstant.StorageType.INTERNAL_DICM
                }
                R.id.rb_internal_picture -> {
                    filePath = File("")
                    storageType = AppConstant.StorageType.INTERNAL_PICTURE
                }
            }

            updateData()
        }
    }

    override fun onClick(v: View) {
        super.onClick(v)
        when (v.id) {
            R.id.btn_create_folder -> {
                createFolder()
            }
            R.id.btn_save_image -> {
                if (filePath == null) {
                    showToast(getString(R.string.please_select_path))
                } else {
                    saveImage(filePath!!)
                }
            }
        }
    }

    private fun updateData() {
        mBinding.btnCreateFolder.visibility = View.GONE
        mBinding.txtFolderPath.visibility = View.GONE
        when (storageType) {
            AppConstant.StorageType.EXTERNAL_STORAGE -> {
                mBinding.btnCreateFolder.visibility = View.VISIBLE
                mBinding.txtFolderPath.visibility = View.VISIBLE
                mBinding.txtStoragePath.text = getString(R.string.external_storage, packageName)
            }
            AppConstant.StorageType.INTERNAL_DOWNLOAD -> {
                mBinding.txtStoragePath.text = getString(R.string.internal_download_folder)
            }
            AppConstant.StorageType.INTERNAL_DICM -> {
                mBinding.txtStoragePath.text = getString(R.string.internal_dicm_folder)
            }
            AppConstant.StorageType.INTERNAL_PICTURE -> {
                mBinding.txtStoragePath.text = getString(R.string.internal_picture_folder)
            }
        }
        mBinding.txtFilePath.text = ""
        mBinding.txtFolderPath.text = ""
    }

    private fun createFolder() {
        when (storageType) {
            AppConstant.StorageType.EXTERNAL_STORAGE -> {
// Folder exact path is - /storage/emulated/0/Android/data/com.webaddicted.imagepickercompressor/files/image.jpg
                val directFile = getExternalFilesDir(null)
// This path create Download folder on Android > Data> packageName > files> Folder
// Folder exact path is - /storage/emulated/0/Android/data/com.webaddicted.imagepickercompressor/files/Download/image.jpg
                val downloadFolderFilePath = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
// Folder exact path is - /storage/emulated/0/Android/data/com.webaddicted.imagepickercompressor/files/Download/My Folder/image.jpg
                val myFolderFilePath =
                    File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "My Folder")
                if (!downloadFolderFilePath?.exists()!!) {
                    val created = downloadFolderFilePath.mkdir()
                    showToast("Created Image ${downloadFolderFilePath.absolutePath} = $created")
                } else {
                    showToast("Already present Image ${downloadFolderFilePath.absolutePath}")
                }
                filePath = downloadFolderFilePath
                mBinding.txtFolderPath.text =
                    "Created Image ${downloadFolderFilePath?.absolutePath}"
//                same we can use for externalCacheDir
            }
//            AppConstant.StorageType.INTERNAL_DOWNLOAD -> {
//                mBinding.txtStoragePath.text = getString(R.string.internal_download_folder)
//            }
//            AppConstant.StorageType.INTERNAL_DICM -> {
//                mBinding.txtStoragePath.text = getString(R.string.internal_dicm_folder)
//            }
//            AppConstant.StorageType.INTERNAL_PICTURE -> {
//                saveImageToStorage(bitmap = mBinding.nestedParent.drawToBitmap(), filename = "Picture ${System.currentTimeMillis()}.jpg")
//            }
            AppConstant.StorageType.INTERNAL_DOWNLOAD -> TODO()
            AppConstant.StorageType.INTERNAL_DICM -> TODO()
            AppConstant.StorageType.INTERNAL_PICTURE -> TODO()
        }
        Log.d("TAG", "File Path : ${filePath?.absolutePath}")
    }

    private fun takeScreenshotAndSave(filePath: File) {
        val bitmap = mBinding.nestedParent.drawToBitmap()
        // final output path
        var outputPath = filePath.path
        val imageOutStream = FileOutputStream(filePath)
        imageOutStream.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
    }

    private fun saveImage(filePath: File) {
        when (storageType) {
            AppConstant.StorageType.EXTERNAL_STORAGE -> {
                val file = File(filePath, "ExternalStorage" + System.currentTimeMillis() + ".jpg")
                mBinding.txtFilePath.text = file.absolutePath
                takeScreenshotAndSave(file)
            }
            AppConstant.StorageType.INTERNAL_DOWNLOAD -> {
                val download = DownloadFileFromURLTask(this, getString(R.string.app_name), object : DownloadListener {
                    override fun onSuccess(path: String) {
                        mBinding.txtFilePath.text = path
                    }

                    override fun onFailure(error: String) {
                        mBinding.txtFilePath.text = error
                    }
                })
                download.execute()
            }
            AppConstant.StorageType.INTERNAL_DICM -> {
                saveImageToStorage(
                    bitmap = mBinding.nestedParent.drawToBitmap(),
                    filename = "Picture ${System.currentTimeMillis()}.jpg",
                    directory = Environment.DIRECTORY_DCIM
                )
            }
            AppConstant.StorageType.INTERNAL_PICTURE -> {
                saveImageToStorage(
                    bitmap = mBinding.nestedParent.drawToBitmap(),
                    filename = "Picture ${System.currentTimeMillis()}.jpg",
                    directory = Environment.DIRECTORY_PICTURES
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun saveImageToStorage(
        bitmap: Bitmap,
        outputDir: String = getString(R.string.app_name),
        filename: String = "screenshot.jpg",
        mimeType: String = "image/jpeg",
        directory: String = Environment.DIRECTORY_PICTURES,
    ) {
        val mediaContentUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val imageOutStream: OutputStream
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val outputDirectory = directory + File.separator + outputDir
            // If you want to create custom directory inside Download directory only
//            outputDirectory =
            val desFile = File(outputDirectory)
            if (!desFile.exists()) {
                desFile.mkdir()
            }
            filePath = File(outputDirectory + File.separator + filename)

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, outputDirectory)
            }

            val cr = contentResolver
            cr.run {
                val uri = cr.insert(mediaContentUri, values) ?: return
                imageOutStream = openOutputStream(uri) ?: return
            }
        } else {
            // first we create app name folder direct to the root directory
            var imagePath =
                Environment.getExternalStorageDirectory().path + File.separator + outputDir
            var desFile = File(imagePath)
            if (!desFile.exists()) {
                desFile.mkdir()
            }

            // once the app name directory created we create picture directory inside app directory
            imagePath = imagePath + File.separator + directory
            desFile = File(imagePath)
            if (!desFile.exists()) {
                desFile.mkdir()
            }
            filePath = File(imagePath, filename)
            // final output path
//            filePath = image.path

            imageOutStream = FileOutputStream(filePath)
        }
        mBinding.txtFilePath.text = filePath?.absolutePath.toString()
        imageOutStream.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
    }

}

