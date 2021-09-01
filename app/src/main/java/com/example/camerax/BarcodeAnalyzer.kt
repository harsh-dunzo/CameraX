package com.example.camerax

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
typealias BarcodeListener = (barcode: String) -> Unit

class BarcodeAnalyzer(private val barcodeListener: BarcodeListener) : ImageAnalysis.Analyzer {
    // Get an instance of BarcodeScanner
    private val scanner = BarcodeScanning.getClient()
    private var lastAnalyzedTimestamp = 0L

    companion object{
        private const val TAG = "BarcodeAnalyzer"
    }

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            // Pass image to the scanner and have it do its thing
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    // Task completed successfully
                    processBarcoes(barcodes);
                    //imageProxy.close()
                }
                .addOnFailureListener {
                    Log.d(TAG,it.toString())
                    // You should really do something about Exceptions
                }
                .addOnCompleteListener {
                    // It's important to close the imageProxy
                    imageProxy.close()
                }
        }
    }

    private fun processBarcoes(barcodes: List<Barcode>) {
        for (barcode in barcodes) {
            barcodeListener(barcode.rawValue ?: "")
            //imageProxy.close()
        }
    }
}