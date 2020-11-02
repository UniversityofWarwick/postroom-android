package uk.ac.warwick.postroom

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition

class OcrImageAnalyzer(private val requireContext: Context) : ImageAnalysis.Analyzer {


    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        Log.i(TAG, "Got frame to analyze ")

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val recognizer = TextRecognition.getClient()
            val result = recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    imageProxy.close()
                    val resultText = visionText.text
                    for (block in visionText.textBlocks) {
                        val blockText = block.text
                        val blockCornerPoints = block.cornerPoints
                        val blockFrame = block.boundingBox
                        for (line in block.lines) {
                            val lineText = line.text
                            if (lineText.contains("QE")) {
                                val builder: AlertDialog.Builder =
                                    AlertDialog.Builder(this.requireContext)
                                builder.setTitle(lineText).create().show()
                            }
                            val lineCornerPoints = line.cornerPoints
                            val lineFrame = line.boundingBox
                            for (element in line.elements) {
                                val elementText = element.text
                                val elementCornerPoints = element.cornerPoints
                                val elementFrame = element.boundingBox
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    imageProxy.close()

                    Log.e(TAG, e.message, e)
                }
        }
    }
}
