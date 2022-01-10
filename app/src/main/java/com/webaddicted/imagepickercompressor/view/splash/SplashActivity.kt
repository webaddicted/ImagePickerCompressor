package com.webaddicted.imagepickercompressor.view.splash

import android.app.Activity
import android.content.Intent
import androidx.databinding.ViewDataBinding
import com.webaddicted.imagepickercompressor.R
import com.webaddicted.imagepickercompressor.databinding.ActivitySplashBinding
import com.webaddicted.imagepickercompressor.utils.common.GlobalUtils
import com.webaddicted.imagepickercompressor.view.base.BaseActivity
import com.webaddicted.imagepickercompressor.view.imgpicker.ImagePickerActivity

class SplashActivity : BaseActivity(R.layout.activity_splash) {
    private lateinit var mBinding: ActivitySplashBinding

    companion object {
        val TAG = SplashActivity::class.qualifiedName
        fun newIntent(activity: Activity) {
            activity.startActivity(Intent(activity, SplashActivity::class.java))
        }

        fun newClearLogin(context: Activity?) {
            val intent = Intent(context, SplashActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            context?.startActivity(intent)
            context?.finish()
        }
    }

    override fun onBindTo(binding: ViewDataBinding) {
        mBinding = binding as ActivitySplashBinding
        init()
    }

    private fun init() {
        GlobalUtils.delay(2) { ->
            ImagePickerActivity.newClearLogin(this)
        }
    }
}