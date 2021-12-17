package com.webaddicted.imagepickercompressor.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.webaddicted.imagepickercompressor.utils.common.ImagePickerHelper
import com.webaddicted.imagepickercompressor.utils.common.PermissionHelper

abstract class BaseActivity(private val layoutId: Int) : AppCompatActivity(), View.OnClickListener {

    companion object {
        val TAG = BaseActivity::class.qualifiedName
    }

    abstract fun onBindTo(binding: ViewDataBinding)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ViewDataBinding?

        if (layoutId != 0) {
            try {
                binding = DataBindingUtil.setContentView(this, layoutId)
                onBindTo(binding)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onClick(v: View) {}

    fun navigateFragment(
        layoutContainer: Int,
        fragment: Fragment,
        isEnableBackStack: Boolean = false
    ) {

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(layoutContainer, fragment)
        if (isEnableBackStack)
            fragmentTransaction.addToBackStack(fragment::class.java.simpleName)
        fragmentTransaction.commitAllowingStateLoss()

    }

    fun navigateAddFragment(
        layoutContainer: Int,
        fragment: Fragment,
        isEnableBackStack: Boolean = true
    ) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
//    fragmentTransaction.setCustomAnimations(R.anim.trans_left_in, R.anim.trans_left_out, R.anim.trans_right_in, R.anim.trans_right_out)
//    fragmentTransaction.setCustomAnimations(
//      R.animator.fragment_slide_left_enter,
//      R.animator.fragment_slide_left_exit,
//      R.animator.fragment_slide_right_enter,
//      R.animator.fragment_slide_right_exit
//    )
        fragmentTransaction.add(layoutContainer, fragment)
        if (isEnableBackStack)
            fragmentTransaction.addToBackStack(fragment::class.java.simpleName)
        fragmentTransaction.commitAllowingStateLoss()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHelper.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ImagePickerHelper.ImgPickerType.OPEN_CAMERA.value||requestCode == ImagePickerHelper.ImgPickerType.SELECT_IMAGE.value ||requestCode == ImagePickerHelper.ImgPickerType.CHOOSER_CAMERA_GALLERY.value)
            ImagePickerHelper.onActivityResult(this, requestCode, resultCode, data)
    }

}