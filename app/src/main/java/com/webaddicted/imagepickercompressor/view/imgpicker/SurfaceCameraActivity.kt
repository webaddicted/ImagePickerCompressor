package com.webaddicted.imagepickercompressor.view.imgpicker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.databinding.ViewDataBinding
import com.webaddicted.imagepickercompressor.R
import com.webaddicted.imagepickercompressor.databinding.ActivitySurfaceCameraBinding
import com.webaddicted.imagepickercompressor.utils.common.PermissionHelper
import com.webaddicted.imagepickercompressor.view.base.BaseActivity
import com.webaddicted.imagepickercompressor.view.splash.SplashActivity
import java.io.File
import java.io.FileOutputStream
import java.util.*

class SurfaceCameraActivity : BaseActivity(R.layout.activity_surface_camera) {
    private lateinit var mBinding: ActivitySurfaceCameraBinding
    private var imageDimensions: Size? = null
    private var cameraDevice: CameraDevice? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var backgroundHandler: Handler? = null
    private var handlerThread: HandlerThread? = null
    private var isSurfaceAvailable = false

    companion object {
        val TAG = SplashActivity::class.qualifiedName
        fun newIntent(activity: Activity) {
            activity.startActivity(Intent(activity, SurfaceCameraActivity::class.java))
        }

        fun newClearLogin(context: Activity?) {
            val intent = Intent(context, SurfaceCameraActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            context?.startActivity(intent)
            context?.finish()
        }
    }

    override fun onBindTo(binding: ViewDataBinding) {
        mBinding = binding as ActivitySurfaceCameraBinding
        init()
        clickListener()
    }

    private fun init() {
        mBinding.textureView.surfaceTextureListener = surfaceTextureListener
    }

    private fun clickListener() {
        mBinding.btnCaptureImg.setOnClickListener(this)
        mBinding.btnCircleCapture.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        super.onClick(v)
        when (v.id) {
            R.id.btn_capture_img -> {
                checkPermission()
            }
            R.id.btn_circle_capture -> {
                checkPermission()
            }
        }
    }

    private fun checkPermission() {
        val locationList = ArrayList<String>()
        locationList.add(Manifest.permission.CAMERA)
        locationList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        locationList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        PermissionHelper.requestMultiplePermission(this, locationList)
        { isPermissionGranted: Boolean, _: List<String> ->
            if (isPermissionGranted) {
                if (mBinding.imgCapture.visibility === View.VISIBLE) {
                    mBinding.imgCapture.visibility = View.GONE
                    mBinding.btnCircleCapture.background = getDrawable(R.drawable.drawable_tick)
                    openCameraOnClick()
                } else {
                    mBinding.imgCapture.visibility = View.VISIBLE
                    mBinding.btnCircleCapture.background = getDrawable(R.drawable.ic_camera)
                    mBinding.imgCapture.setImageBitmap(mBinding.textureView.bitmap)
                    closeCameraOnClick()
                }
            }
        }
    }

    private fun openCameraOnClick() {
        startBackgroundThread()
        if (mBinding.textureView.isAvailable) {
            try {
                openCamera()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            mBinding.textureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    private var surfaceTextureListener: TextureView.SurfaceTextureListener = object :
        TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            isSurfaceAvailable = true
        }

        override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            isSurfaceAvailable = false
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    @Throws(CameraAccessException::class)
    private fun openCamera() {
        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[1]
        val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
        val streamConfigurationMap =
            cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        imageDimensions = streamConfigurationMap!!.getOutputSizes(SurfaceTexture::class.java)[0]
        cameraManager.openCamera(cameraId, stateCallback, null)
    }

    private fun closeCameraOnClick() {
        try {
            handlerThread!!.quitSafely()
            handlerThread!!.join()
            backgroundHandler = null
            handlerThread = null
            cameraDevice!!.close()
            cameraDevice = null
            val directFile = getExternalFilesDir(null)
            directFile?.mkdirs()
            val file = File(directFile, "Selfie_" + System.currentTimeMillis() + ".jpg")
            val imageOutStream = FileOutputStream(file)
            val bitmap = mBinding.textureView.bitmap
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, imageOutStream)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private var stateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            try {
                startCameraPreview()
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice!!.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice!!.close()
            cameraDevice = null
        }
    }

    @Throws(CameraAccessException::class)
    private fun startCameraPreview() {
        val texture = mBinding.textureView.surfaceTexture
        texture!!.setDefaultBufferSize(imageDimensions!!.width, imageDimensions!!.height)
        val surface = Surface(texture)
        captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder!!.addTarget(surface)
        cameraDevice?.createCaptureSession(
            Collections.singletonList(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    cameraCaptureSession = session
                    try {
                        updatePreview()
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    val activity = applicationContext as Activity
                    if (null != activity) {
                        Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }, backgroundHandler
        )
    }

    @Throws(CameraAccessException::class)
    private fun updatePreview() {
        if (cameraDevice == null) {
            return
        }
        captureRequestBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        cameraCaptureSession?.setRepeatingRequest(
            captureRequestBuilder!!.build(),
            null,
            backgroundHandler
        )
    }


//    override fun onResume() {
//        super.onResume()
//        startBackgroundThread()
//        if (textureView!!.isAvailable) {
//            try {
//                openCamera()
//            } catch (e: CameraAccessException) {
//                e.printStackTrace()
//            }
//        } else {
//            textureView!!.surfaceTextureListener = surfaceTextureListener
//        }
//    }
//
//    override fun onPause() {
//        try {
//            stopBackgroundThread()
//        } catch (e: InterruptedException) {
//            e.printStackTrace()
//        }
//        super.onPause()
//    }

    private fun startBackgroundThread() {
        handlerThread = HandlerThread("Camera Background")
        handlerThread!!.start()
        backgroundHandler = Handler(handlerThread!!.looper)
    }

    @Throws(InterruptedException::class)
    private fun stopBackgroundThread() {
        handlerThread!!.quitSafely()
        handlerThread!!.join()
        backgroundHandler = null
        handlerThread = null
    }

    fun takePicture() {
        if (isSurfaceAvailable) {
            openCamera()
        }
    }

}