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
    var imageMimeTypes = arrayOf("image/jpeg", "image/png", "image/jpg")

    companion object {
        val TAG = CameraXActivity::class.java.simpleName.toString()
        const val FRONT_IMG = "FrontImg"
        const val BACK_IMG = "BackImg"
        const val SCREEN_OPEN_FROM = "ScreenOpenFrom"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val CAMERA_TYPE = "CameraType"
        const val AADHAAR_CARD = 9001
        const val PAN_CARD = 9002

        @JvmStatic
        fun newIntent(
            activity: Activity,
            cameraType: Int = AADHAAR_CARD
        ): Intent {
            val intent = Intent(activity, CameraXActivity::class.java)
            intent.putExtra(CAMERA_TYPE, cameraType)
            return intent
        }

        fun newClearLogin(context: Activity?) {
            val intent = Intent(context, CameraXActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            context?.startActivity(intent)
            context?.finish()
        }
    }

    override fun onBindTo(binding: ViewDataBinding) {
        mBinding = binding as ActivityCameraHelperBinding
        supportActionBar?.hide()
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = Color.TRANSPARENT
        }
        cameraType = intent.getIntExtra(CAMERA_TYPE, AADHAAR_CARD)
        initUi()
    }

    private fun initUi() {
        mBinding.includeDocCapture.imgBack.setOnClickListener(this)
        mBinding.includeDocCapture.imgCapture.setOnClickListener(this)
        mBinding.includeDocCapture.imgSelfie.setOnClickListener(this)
        mBinding.includeDocCapture.imgGallery.setOnClickListener(this)
        mBinding.includeDocCapture.btnConfirm.setOnClickListener(this)
        mBinding.includeDocCapture.btnRetake.setOnClickListener(this)
        mBinding.includeDocCapture.imgOption.setOnClickListener(this)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }


    override fun onClick(view: View) {
        super.onClick(view)
        when (view.id) {
            R.id.img_back -> onBackPressed()
            R.id.img_capture -> takePhoto()
            R.id.img_selfie -> {
                lensFacing = when (lensFacing) {
                    CameraSelector.LENS_FACING_BACK -> CameraSelector.LENS_FACING_FRONT
                    else -> CameraSelector.LENS_FACING_BACK
                }
                startCamera()
            }
            R.id.img_gallery -> {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                intent.putExtra(Intent.EXTRA_MIME_TYPES, imageMimeTypes)
                imgPickerLauncher.launch(Intent.createChooser(intent, "Select Picture"))
            }
            R.id.btn_confirm -> confirmClick()
            R.id.btn_retake -> {
                deleteImage()
                startCamera()
            }
            R.id.img_option -> showPopupMenu(mBinding.includeDocCapture.imgOption)
        }
    }

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
        val locationList = ArrayList<String>()
        locationList.add(Manifest.permission.CAMERA)
        PermissionHelper.requestMultiplePermission(this, locationList)
        { isPermissionGranted: Boolean, _: List<String> ->
            if (isPermissionGranted) startCamera()
            else {
                showToast(getString(R.string.forcefully_enable_permission))
                finish()
            }
        }
    }

    private fun startCamera() {
        mBinding.imgPreview.visibility = View.GONE
        mBinding.previewView.visibility = View.VISIBLE
        mBinding.includeDocCapture.liCameraPreview.visibility = View.GONE
        mBinding.includeDocCapture.liLiveCamera.visibility = View.VISIBLE
        when (cameraType) {
            AADHAAR_CARD -> {
                mBinding.includeDocCapture.txtTitle.text =
                    if (firstImage == null) getString(R.string.take_aadhaar_front_photo)
                    else getString(R.string.take_aadhaar_back_photo)
            }
            PAN_CARD -> {
                mBinding.includeDocCapture.liLiveCamera.visibility = View.VISIBLE
                mBinding.includeDocCapture.txtTitle.text =
                    getString(R.string.take_pan_photo)
            }
        }
        filePath = null
        outputDirectory = getOutputDirectory()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(mBinding.previewView.surfaceProvider) }
            imageCapture = ImageCapture.Builder().build()
            imageCapture?.flashMode = flashMode
            try {
                cameraProvider.unbindAll()
                camera =
                    cameraProvider.bindToLifecycle(
                        this,
                        createCameraSelector(),
                        preview,
                        imageCapture
                    )
                val control: CameraControl = camera!!.cameraControl
                control.enableTorch(isTorchModeEnabled)
                setupZoomAndTapToFocus(camera!!.cameraInfo, control)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun createCameraSelector(): CameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    shutterSound()
                    mBinding.imgPreview.setImageURI(savedUri)
                    filePath = savedUri.path?.let { File(it) }
                    capturedImagePreview()
                }
            })
    }

    private fun capturedImagePreview() {
        mBinding.previewView.visibility = View.GONE
        mBinding.imgPreview.visibility = View.VISIBLE
        mBinding.includeDocCapture.liLiveCamera.visibility = View.GONE
        mBinding.includeDocCapture.liCameraPreview.visibility = View.VISIBLE

        when (cameraType) {
            AADHAAR_CARD -> {
                mBinding.includeDocCapture.txtTitle.text =
                    if (firstImage == null) getString(R.string.confirm_aadhaar_front_photo)
                    else getString(R.string.confirm_aadhaar_back_photo)
            }
            PAN_CARD -> {
                mBinding.includeDocCapture.txtTitle.text =
                    getString(R.string.confirm_pan_photo)
            }
        }
    }

    private val imgPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data != null && data.data != null) {
                    getImgFromGallery = true
                    val imgUri = data.data as Uri
                    mBinding.imgPreview.setImageURI(imgUri)
                    val bmp = getBitmapFromUri(this, imgUri)
                    filePath = getFile(this, bmp)
                    capturedImagePreview()
                }
            }
        }

    //    val intent = Intent()
//    intent.putExtra(IS_SUCCESSFULLY_IMAGE_VERIFY, true)
//    setResult(RESULT_OK, intent)
//    super.finish()
    private fun confirmClick() {
        when (cameraType) {
            AADHAAR_CARD -> {
                if (firstImage == null) {
                    firstImage = filePath
                    mBinding.includeDocCapture.txtTitle.text =
                        getString(R.string.take_aadhaar_back_photo)
                    startCamera()
                } else selectedImage()
            }
            PAN_CARD -> {
                firstImage = filePath
                filePath = null
                selectedImage()
            }
        }
    }

    private fun selectedImage() {
        val data = Intent()
        data.putExtra(FRONT_IMG, firstImage?.path.toString())
        if (filePath != null)
            data.putExtra(BACK_IMG, filePath?.path.toString())
        data.putExtra(CAMERA_TYPE, cameraType)
        data.putExtra(SCREEN_OPEN_FROM, TAG)
        setResult(RESULT_OK, data)
        finish()
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

    private fun getFile(mContext: Context, bmp: Bitmap?): File? {
        val file = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )
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

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs?.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    private fun setupZoomAndTapToFocus(
        cameraInfo: CameraInfo,
        cameraControl: CameraControl,
    ) {
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentZoomRatio: Float = cameraInfo.zoomState.value?.zoomRatio ?: 1F
                val delta = detector.scaleFactor
                cameraControl.setZoomRatio(currentZoomRatio * delta)
                return true
            }
        }
        val scaleGestureDetector = ScaleGestureDetector(mBinding.previewView.context, listener)
        mBinding.previewView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_DOWN) {
                val factory = mBinding.previewView.meteringPointFactory
                val point = factory.createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                    .setAutoCancelDuration(5, TimeUnit.SECONDS)
                    .build()
                cameraControl.startFocusAndMetering(action)
            }
            true
        }
    }

    private fun shutterSound() {
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        when (audio.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                val sound = MediaActionSound()
                sound.play(MediaActionSound.SHUTTER_CLICK)
            }
            AudioManager.RINGER_MODE_SILENT -> {}
            AudioManager.RINGER_MODE_VIBRATE -> {}
        }
    }

    private fun deleteImage() {
        if (filePath!!.exists()) filePath!!.delete()
    }

    private fun showPopupMenu(view: View) { // inflate menu
        val popup = PopupMenu(this, view)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.menu_camera, popup.menu)
        val flash: MenuItem = popup.menu.findItem(R.id.flash)
        val torch: MenuItem = popup.menu.findItem(R.id.torch)
        popup.setForceShowIcon(true)
        if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            isTorchModeEnabled = false
            flashMode = ImageCapture.FLASH_MODE_OFF
        }
        if (isTorchModeEnabled) {
            torch.title = getString(R.string.torch_off)
            torch.icon = getDrawable(R.drawable.ic_torch_off)
        } else {
            torch.title = getString(R.string.torch_on)
            torch.icon = getDrawable(R.drawable.ic_torch_on)
        }
        when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> {
                flash.title = getString(R.string.flash_on)
                flash.icon = getDrawable(R.drawable.ic_flash_on)
            }
            ImageCapture.FLASH_MODE_ON -> {
                flash.title = getString(R.string.flash_auto)
                flash.icon = getDrawable(R.drawable.ic_flash_auto)
            }
            else -> {
                flash.title = getString(R.string.flash_off)
                flash.icon = getDrawable(R.drawable.ic_flash_off)
            }
        }
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.flash -> {
                    if (lensFacing != CameraSelector.LENS_FACING_FRONT) {
                        if (camera?.cameraInfo?.hasFlashUnit() == true) {
                            flashMode = when (flashMode) {
                                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                                else -> ImageCapture.FLASH_MODE_OFF
                            }
                            imageCapture?.flashMode = flashMode
                        }
                    }
                }
                R.id.torch -> {
                    if (lensFacing != CameraSelector.LENS_FACING_FRONT) {
                        isTorchModeEnabled = !isTorchModeEnabled
                        camera?.cameraControl?.enableTorch(isTorchModeEnabled)
                    }
                }
                else -> showToast("Other menu click")
            }
            true
        }
        popup.show()
    }
}

