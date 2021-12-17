package com.webaddicted.imagepickercompressor.view

import android.graphics.Bitmap
import android.view.View
import androidx.databinding.ViewDataBinding
import com.webaddicted.imagepickercompressor.R
import com.webaddicted.imagepickercompressor.databinding.ActivityMainBinding
import com.webaddicted.imagepickercompressor.utils.common.ImagePickerHelper
import java.io.File

class MainActivity : BaseActivity(R.layout.activity_main) {
    private lateinit var mBinding: ActivityMainBinding

    override fun onBindTo(binding: ViewDataBinding) {
        mBinding = binding as ActivityMainBinding
        clickListener()
    }

    private fun clickListener() {
        mBinding.btnCaptureImg.setOnClickListener(this)
        mBinding.btnGalleryPick.setOnClickListener(this)
        mBinding.btnChooseOption.setOnClickListener(this)
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