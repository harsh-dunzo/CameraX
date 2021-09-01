package com.example.camerax

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.camera.core.impl.ImageAnalysisConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import android.hardware.camera2.CaptureRequest
import android.util.DisplayMetrics
import android.util.Range
import android.util.Size

import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.*
import java.lang.Math.abs
import java.util.Collections.max


class MainActivity : AppCompatActivity() {

    private var imageCapture: ImageCapture? = null

    private var processingBarcode = AtomicBoolean(false)
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto() {}

    @SuppressLint("RestrictedApi")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()


            val builder: ImageAnalysis.Builder = ImageAnalysis.Builder()
            val ext: Camera2Interop.Extender<*> = Camera2Interop.Extender(builder)
            ext.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                Range<Int>(10, 10)
            )

            val imageAnalysis = builder
                .setTargetAspectRatio(aspectRatio())
                .setTargetRotation(viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setImageQueueDepth(10)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcode ->
                        // if (processingBarcode.compareAndSet(false, true)) {
                        searchBarcode(barcode)
                        // }
                    })

                }

            //val imageAnalysis: ImageAnalysis = builder.build()

            // Preview
            val preview = Preview.Builder()
                .setDefaultResolution(Size(2688,1520))
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            val displayMetrics = resources.displayMetrics
            val factory = SurfaceOrientedMeteringPointFactory(
                displayMetrics.widthPixels.toFloat(),
                displayMetrics.heightPixels.toFloat()
            )
            val point = factory.createPoint(
                displayMetrics.widthPixels / 2f,
                displayMetrics.heightPixels / 2f
            )
            val action = FocusMeteringAction
                .Builder(point, FocusMeteringAction.FLAG_AF)
                .build()



            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA


            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
               val camera= cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview,imageAnalysis)

                //can add coroutines here

                camera.cameraControl.startFocusAndMetering(action)
                camera.cameraControl.enableTorch(true)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun aspectRatio(): Int {
        with(DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }) {
            val previewRatio = Math.max(widthPixels, heightPixels).toDouble() / widthPixels.coerceAtMost(
                heightPixels
            )
            if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
                return AspectRatio.RATIO_4_3
            }
            return AspectRatio.RATIO_16_9
        }

    }

    private fun searchBarcode(barcode: String) {
        //Snackbar.make(baseContext,barcode,6000L)
        val snackbar = Snackbar.make(root,barcode,Snackbar.LENGTH_LONG)
        snackbar.setAction("DISMISS",{
            snackbar.dismiss()
        })
        snackbar.show()


        //Toast.makeText(this,barcode,Toast.LENGTH_SHORT).show()
        //Log.d(TAG,barcode)
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }

    }
}