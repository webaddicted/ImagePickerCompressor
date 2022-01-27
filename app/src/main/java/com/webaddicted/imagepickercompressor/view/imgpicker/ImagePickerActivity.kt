package com.webaddicted.imagepickercompressor.view.imgpicker

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import androidx.databinding.ViewDataBinding
import com.webaddicted.imagepickercompressor.R
import com.webaddicted.imagepickercompressor.databinding.ActivityImgPickerBinding
import com.webaddicted.imagepickercompressor.utils.common.ImagePickerHelper
import com.webaddicted.imagepickercompressor.view.base.BaseActivity
import com.webaddicted.imagepickercompressor.view.splash.SplashActivity
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
        }
    }

    private fun captureImg(imageType: ImagePickerHelper.ImgPickerType) {
        ImagePickerHelper.getImage(
            this,
            imagePickerLauncher,
            imageType
        ) { mFile: File, imageBitmap: Bitmap ->
            if (mFile.exists()) mBinding.imgPick.setImageBitmap(imageBitmap)
        }

    }
}