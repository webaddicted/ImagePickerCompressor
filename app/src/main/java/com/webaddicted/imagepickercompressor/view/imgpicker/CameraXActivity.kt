package com.webaddicted.imagepickercompressor.view.imgpicker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaActionSound
import android.net.Uri
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import com.webaddicted.imagepickercompressor.R
import com.webaddicted.imagepickercompressor.databinding.ActivityCameraHelperBinding
import com.webaddicted.imagepickercompressor.utils.common.GlobalUtils.showToast
import com.webaddicted.imagepickercompressor.utils.common.PermissionHelper
import com.webaddicted.imagepickercompressor.view.base.BaseActivity
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraXActivity : BaseActivity(R.layout.activity_camera_helper) {
    private var camera: Camera? = null
    private var flashMode = ImageCapture.FLASH_MODE_OFF
    private var isTorchModeEnabled = false
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var getImgFromGallery: Boolean = false
    private lateinit var mBinding: ActivityCameraHelperBinding
    private var imageCapture: ImageCapture? = null
    private var filePath: File? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private var cameraType: Int = AADHAAR_CARD
    private var firstImage: File? = null

    companion object {
        private const val TAG = "CameraXActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val FRONT_IMG = "FrontImg"
        const val BACK_IMG = "BackImg"
        const val SCREEN_OPEN_FROM = "ScreenOpenFrom"
        const val CAMERA_TYPE = "CameraType"
        const val AADHAAR_CARD = 9001
        const val PAN_CARD = 9002

        @JvmStatic
        fun newIntent(activity: Activity, cameraType: Int = AADHAAR_CARD): Intent {
            return Intent(activity, CameraXActivity::class.java).apply {
                putExtra(CAMERA_TYPE, cameraType)
            }
        }

        fun newClearLogin(context: Activity?) {
            val intent = Intent(context, CameraXActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context?.startActivity(intent)
            context?.finish()
        }
    }

    override fun onBindTo(binding: ViewDataBinding) {
        mBinding = binding as ActivityCameraHelperBinding
        setupActionBarAndWindow()
        cameraType = intent.getIntExtra(CAMERA_TYPE, AADHAAR_CARD)
        initUi()
    }

    private fun setupActionBarAndWindow() {
        supportActionBar?.hide()
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = Color.TRANSPARENT
        }
    }

    private fun initUi() {
        with(mBinding.includeDocCapture) {
            imgBack.setOnClickListener { onBackPressed() }
            imgCapture.setOnClickListener { takePhoto() }
            imgSelfie.setOnClickListener { toggleCameraLens() }
            imgGallery.setOnClickListener { openGallery() }
            btnConfirm.setOnClickListener { confirmClick() }
            btnRetake.setOnClickListener { retakePhoto() }
            imgOption.setOnClickListener { showPopupMenu(it) }
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onClick(view: View) {}

    override fun onBackPressed() {
        if (cameraType == AADHAAR_CARD && firstImage != null) {
            firstImage = null
            startCamera()
        } else super.onBackPressed()
    }

    override fun onStop() {
        super.onStop()
        cameraExecutor.shutdown()
    }

    override fun onResume() {
        super.onResume()
        if (getImgFromGallery) {
            getImgFromGallery = false
            return
        }
        PermissionHelper.requestMultiplePermission(this, listOf(Manifest.permission.CAMERA)) { granted, _ ->
            if (granted) startCamera() else {
                showToast(getString(R.string.forcefully_enable_permission))
                finish()
            }
        }
    }

    private fun startCamera() {
        resetCameraUi()
        outputDirectory = getOutputDirectory()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = mBinding.previewView.surfaceProvider }

            imageCapture = ImageCapture.Builder().setFlashMode(flashMode).build()

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, createCameraSelector(), preview, imageCapture).apply {
                    cameraControl.enableTorch(isTorchModeEnabled)
                }
                setupZoomAndTapToFocus(camera!!.cameraInfo, camera!!.cameraControl)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun resetCameraUi() {
        with(mBinding) {
            imgPreview.visibility = View.GONE
            previewView.visibility = View.VISIBLE
            includeDocCapture.liCameraPreview.visibility = View.GONE
            includeDocCapture.liLiveCamera.visibility = View.VISIBLE
        }

        when (cameraType) {
            AADHAAR_CARD -> mBinding.includeDocCapture.txtTitle.text =
                getString(if (firstImage == null) R.string.take_aadhaar_front_photo else R.string.take_aadhaar_back_photo)
            PAN_CARD -> mBinding.includeDocCapture.txtTitle.text = getString(R.string.take_pan_photo)
        }
    }

    private fun createCameraSelector() = CameraSelector.Builder().requireLensFacing(lensFacing).build()

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(outputDirectory, SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg")

        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(photoFile).build(),
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    filePath = photoFile
                    mBinding.imgPreview.setImageURI(Uri.fromFile(photoFile))
                    shutterSound()
                    capturedImagePreview()
                }
            }
        )
    }

    private fun toggleCameraLens() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        startCamera()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "image/jpg"))
        }
        imgPickerLauncher.launch(Intent.createChooser(intent, getString(R.string.select_picture)))
    }

    private val imgPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    getImgFromGallery = true
                    mBinding.imgPreview.setImageURI(uri)
                    filePath = getFileFromBitmap(getBitmapFromUri(uri))
                    capturedImagePreview()
                }
            }
        }

    private fun confirmClick() {
        if (cameraType == AADHAAR_CARD) {
            if (firstImage == null) {
                firstImage = filePath
                startCamera()
            } else finishActivityWithResult()
        } else {
            firstImage = filePath
            finishActivityWithResult()
        }
    }

    private fun retakePhoto() {
        filePath?.delete()
        startCamera()
    }

    private fun capturedImagePreview() {
        with(mBinding) {
            previewView.visibility = View.GONE
            imgPreview.visibility = View.VISIBLE
            includeDocCapture.liLiveCamera.visibility = View.GONE
            includeDocCapture.liCameraPreview.visibility = View.VISIBLE
        }
    }

    private fun finishActivityWithResult() {
        setResult(RESULT_OK, Intent().apply {
            putExtra(FRONT_IMG, firstImage?.path)
            putExtra(BACK_IMG, filePath?.path)
            putExtra(CAMERA_TYPE, cameraType)
            putExtra(SCREEN_OPEN_FROM, TAG)
        })
        finish()
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }

    private fun getFileFromBitmap(bitmap: Bitmap?): File? {
        val file = File(outputDirectory, SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg")
        file.createNewFile()
        FileOutputStream(file).use { bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        return file
    }

    private fun getOutputDirectory(): File {
        return externalMediaDirs.firstOrNull()?.let {
            File(it, getString(R.string.app_name)).apply { mkdirs() }
        } ?: filesDir
    }

    private fun setupZoomAndTapToFocus(cameraInfo: CameraInfo, cameraControl: CameraControl) {
        val scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val zoomRatio = cameraInfo.zoomState.value?.zoomRatio ?: 1f
                cameraControl.setZoomRatio(zoomRatio * detector.scaleFactor)
                return true
            }
        })

        mBinding.previewView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_DOWN) {
                val point = mBinding.previewView.meteringPointFactory.createPoint(event.x, event.y)
                cameraControl.startFocusAndMetering(
                    FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF).setAutoCancelDuration(5, TimeUnit.SECONDS).build()
                )
            }
            true
        }
    }

    private fun shutterSound() {
        (getSystemService(AUDIO_SERVICE) as AudioManager).apply {
            if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                MediaActionSound().play(MediaActionSound.SHUTTER_CLICK)
            }
        }
    }

    private fun File?.delete() {
        this?.takeIf { it.exists() }?.delete()
    }

    private fun showPopupMenu(view: View) {
        PopupMenu(this, view).apply {
            menuInflater.inflate(R.menu.menu_camera, menu)
            setForceShowIcon(true)

            with(menu) {
                findItem(R.id.flash).apply {
                    title = getString(
                        when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> R.string.flash_on
                            ImageCapture.FLASH_MODE_ON -> R.string.flash_auto
                            else -> R.string.flash_off
                        }
                    )
                    icon = getDrawable(
                        when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> R.drawable.ic_flash_on
                            ImageCapture.FLASH_MODE_ON -> R.drawable.ic_flash_auto
                            else -> R.drawable.ic_flash_off
                        }
                    )
                }
                findItem(R.id.torch).apply {
                    title = getString(if (isTorchModeEnabled) R.string.torch_off else R.string.torch_on)
                    icon = getDrawable(if (isTorchModeEnabled) R.drawable.ic_torch_off else R.drawable.ic_torch_on)
                }
            }

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.flash -> toggleFlashMode()
                    R.id.torch -> toggleTorchMode()
                    else -> showToast("Other menu click")
                }
                true
            }
        }.show()
    }

    private fun toggleFlashMode() {
        if (lensFacing != CameraSelector.LENS_FACING_FRONT && camera?.cameraInfo?.hasFlashUnit() == true) {
            flashMode = when (flashMode) {
                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                else -> ImageCapture.FLASH_MODE_OFF
            }
            imageCapture?.flashMode = flashMode
        }
    }

    private fun toggleTorchMode() {
        if (lensFacing != CameraSelector.LENS_FACING_FRONT) {
            isTorchModeEnabled = !isTorchModeEnabled
            camera?.cameraControl?.enableTorch(isTorchModeEnabled)
        }
    }
}
