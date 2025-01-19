package com.webaddicted.imagepickercompressor.view.imgpicker

import android.Manifest
import android.app.Activity
import android.app.ComponentCaller
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import androidx.camera.core.impl.Observable.Observer
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.coroutineScope
import com.google.android.gms.common.util.DataUtils
import com.webaddicted.imagepickercompressor.BuildConfig
import com.webaddicted.imagepickercompressor.R
import com.webaddicted.imagepickercompressor.databinding.ActivityImgPickerBinding
import com.webaddicted.imagepickercompressor.utils.common.GlobalUtils.showToast
import com.webaddicted.imagepickercompressor.utils.common.ImagePickerHelper
import com.webaddicted.imagepickercompressor.utils.common.ImagePickerHelper.convertBase64Image
import com.webaddicted.imagepickercompressor.utils.common.ImageUtils
import com.webaddicted.imagepickercompressor.utils.common.LocationLiveData
import com.webaddicted.imagepickercompressor.utils.common.PermissionHelper
import com.webaddicted.imagepickercompressor.utils.common.Status
import com.webaddicted.imagepickercompressor.view.base.BaseActivity
import com.webaddicted.imagepickercompressor.view.splash.SplashActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ImagePickerActivity : BaseActivity(R.layout.activity_img_picker) {
    private lateinit var mBinding: ActivityImgPickerBinding

    companion object {
        val TAG = SplashActivity::class.qualifiedName
        fun newIntent(activity: Activity) {
            activity.startActivity(Intent(activity, ImagePickerActivity::class.java))
        }

        fun newClearLogin(context: Activity?) {
            val intent = Intent(context, ImagePickerActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            context?.startActivity(intent)
            context?.finish()
        }
    }

    override fun onBindTo(binding: ViewDataBinding) {
        mBinding = binding as ActivityImgPickerBinding
        clickListener()
    }

    private fun clickListener() {
        mBinding.btnCaptureImg.setOnClickListener(this)
        mBinding.btnGalleryPick.setOnClickListener(this)
        mBinding.btnChooseOption.setOnClickListener(this)
        mBinding.btnScopedStorage.setOnClickListener(this)
        mBinding.btnSurfaceCamera.setOnClickListener(this)
        mBinding.btnCameraX.setOnClickListener(this)
        mBinding.btnLocation.setOnClickListener(this)

    }

    override fun onClick(v: View) {
        super.onClick(v)
        when (v.id) {
            R.id.btn_capture_img -> {
                captureImg(ImagePickerHelper.ImgPickerType.OPEN_CAMERA)
            }
            R.id.btn_gallery_pick -> {
                captureImg(ImagePickerHelper.ImgPickerType.SELECT_IMAGE)
            }
            R.id.btn_choose_option -> {
                captureImg(ImagePickerHelper.ImgPickerType.CHOOSER_CAMERA_GALLERY)
            }
            R.id.btn_scoped_storage -> {
                ScopedStorageActivity.newIntent(this)
            }
            R.id.btn_surface_camera -> {
                SurfaceCameraActivity.newIntent(this)
            }
            R.id.btn_camera_x -> {
                val intent = CameraXActivity.newIntent(this)
                startActivity(intent)
            }
            R.id.btnLocation -> {
                PermissionHelper.requestMultiplePermission(this, listOf(Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) { granted, _ ->
                    if (granted){
                        val locationLiveData = LocationLiveData(this)
                        locationLiveData.observe(this) {
                            if (it.status == Status.SUCCESS) {
                                val location = it.data
                                if (location?.latitude != 0.0 && location?.longitude != 0.0) {
                                    mBinding.txtLocation.text = "Latitude:${location?.latitude.toString()} \nLongitude:${location?.longitude.toString()}"
                                }
                            }
                        }
                    }else {
                        showToast(getString(R.string.forcefully_enable_permission))
                    }
                }
            }
        }
    }

    private fun captureImg(imageType: ImagePickerHelper.ImgPickerType) {
        ImagePickerHelper.getImage(
            this,
            imagePickerLauncher,
            imageType
        ) { mFile: File, imageBitmap: Bitmap ->
            if (mFile.exists()) {
                mBinding.imgPick.setImageBitmap(imageBitmap)
                convertImageToBase64(mFile)
            }
        }

    }

    private fun convertImageToBase64(file: java.io.File) {
        lifecycle.coroutineScope.launch(Dispatchers.IO) {
            val firstImg = convertBase64Image(file.path)
            if (BuildConfig.DEBUG) Log.d(
                "Base64Image", "Compressed Image : ${
                    ImageUtils.getBase64ImageSize(
                        firstImg
                    )
                }"
            )
            withContext(Dispatchers.Main) {
                showToast("Image Convert Successfully")
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: ComponentCaller
    ) {
        super.onActivityResult(requestCode, resultCode, data, caller)
        if (resultCode != Activity.RESULT_OK && (requestCode == 6000)){
            showToast(getString(R.string.forcefully_enable_permission))
        }
    }
}