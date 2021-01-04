package uk.ac.warwick.postroom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import uk.ac.warwick.postroom.activities.TAG
import uk.ac.warwick.postroom.domain.BarcodeFormat
import uk.ac.warwick.postroom.domain.RecipientGuess
import uk.ac.warwick.postroom.domain.RecipientGuessType
import uk.ac.warwick.postroom.domain.RecognisedBarcode
import uk.ac.warwick.postroom.vm.CameraViewModel

class OcrImageAnalyzer(
    private val requireContext: Context,
    private val cameraViewModel: CameraViewModel
) : ImageAnalysis.Analyzer {


    var w: Int = 0
    var h: Int = 0

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            handleBarcodes(image, imageProxy)

            val recognizer = TextRecognition.getClient()
            val result = recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    if (h != mediaImage.height) {
                        h = mediaImage.height
                        w = mediaImage.width
                        cameraViewModel.height.value = h
                        cameraViewModel.width.value = w
                    }

                    imageProxy.close()

                    var foundId = false
                    val regexStrict = "(?<![\\d-])([0-9]{7})(-[A-Za-z])".toRegex()
                    var recipientGuessesSet = cameraViewModel.recipientGuesses.value ?: emptySet()

                    var lastBb: Rect? = null
                    var lastUniId: String? = null
                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            for (element in line.elements) {
                                val trim = element.text.trim()

                                if (cameraViewModel.rooms.value?.containsKey(trim) == true) {
                                    cameraViewModel.room.value = trim
                                    recipientGuessesSet =
                                        recipientGuessesSet.plus(
                                            RecipientGuess(
                                                trim, RecipientGuessType.Room,
                                                cameraViewModel.rooms.value!![trim]
                                                    ?: error("Room entry disappeared")
                                            )
                                        )

                                }

                                if (!foundId && trim.matches("^[0-9]{7}$".toRegex())) {
                                    lastUniId = trim
                                    lastBb = element.boundingBox
                                    foundId = true
                                } else if (!foundId && regexStrict.containsMatchIn(trim)) {
                                    lastUniId = regexStrict.find(trim)!!.groupValues.drop(1).first()
                                    lastBb = element.boundingBox
                                    foundId = true
                                } else if (!foundId) {
                                    val regex = "(?<![\\d-])([0-9]{7})(?![/\\d])".toRegex()
                                    if (regex.containsMatchIn(trim)) {
                                        lastUniId = regex.find(trim)!!.groupValues.drop(1).first()
                                        lastBb = element.boundingBox
                                        foundId = true
                                    }
                                } else if (element.text.contains("\\d".toRegex())) {
                                    Log.i(TAG, element.text)
                                }
                                val elementText = element.text
                                val elementCornerPoints = element.cornerPoints

                                val elementFrame = element.boundingBox
                            }
                        }
                    }
                    if (foundId && cameraViewModel.uniIds.value?.containsKey(lastUniId!!) == true) {
                        cameraViewModel.uniIdBoundingBox.value = lastBb
                        cameraViewModel.uniId.value = lastUniId!!

                        recipientGuessesSet =
                            recipientGuessesSet.plus(
                                RecipientGuess(
                                    lastUniId, RecipientGuessType.UniversityId,
                                    cameraViewModel.uniIds.value!![lastUniId]
                                        ?: error("Uni ID disappeared")
                                )
                            )

                    } else {
                        cameraViewModel.uniIdBoundingBox.value = null
                    }

                    cameraViewModel.recipientGuesses.postValue(recipientGuessesSet)
                }
                .addOnFailureListener { e ->
                    imageProxy.close()

                    Log.e(TAG, e.message, e)
                }
        }
    }

    private fun handleBarcodes(
        image: InputImage,
        imageProxy: ImageProxy
    ) {
        val scanner = BarcodeScanning.getClient()
        var barcodeSet = cameraViewModel.allCollectedBarcodes.value ?: emptySet()

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val uuid =
                        "[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89aAbB][a-f0-9]{3}-[a-f0-9]{12}".toRegex()
                    if (cameraViewModel.qrId.value != barcode.rawValue && barcode.format == Barcode.FORMAT_QR_CODE && true == barcode.rawValue?.matches(
                            uuid
                        )
                    ) {
                        cameraViewModel.qrId.value = barcode.rawValue!!
                    } else if (barcode.rawValue?.matches(uuid) == false) {
                        val barcodeFormat = when (barcode.format) {
                            Barcode.FORMAT_QR_CODE -> BarcodeFormat.Qr
                            Barcode.FORMAT_AZTEC -> BarcodeFormat.Aztec
                            Barcode.FORMAT_CODABAR -> BarcodeFormat.Codabar
                            Barcode.FORMAT_CODE_128 -> BarcodeFormat.Code128
                            Barcode.FORMAT_CODE_39 -> BarcodeFormat.Code39
                            Barcode.FORMAT_DATA_MATRIX -> BarcodeFormat.DataMatrix
                            Barcode.FORMAT_CODE_93 -> BarcodeFormat.Code93
                            Barcode.FORMAT_EAN_13 -> BarcodeFormat.Ean13
                            Barcode.FORMAT_EAN_8 -> BarcodeFormat.Ean8
                            Barcode.FORMAT_ITF -> BarcodeFormat.Itf
                            Barcode.FORMAT_PDF417 -> BarcodeFormat.Pdf417
                            Barcode.FORMAT_UPC_A -> BarcodeFormat.UpcA
                            Barcode.FORMAT_UPC_E -> BarcodeFormat.UpcE
                            else -> null
                        }

                        barcodeSet = barcodeSet.plus(
                            RecognisedBarcode(barcodeFormat, barcode.rawValue!!)
                        )

                    }
                }
                cameraViewModel.allCollectedBarcodes.postValue(barcodeSet)
                cameraViewModel.barcodes.value = barcodes.size
            }
            .addOnFailureListener { e ->
                imageProxy.close()

                Log.e(TAG, e.message, e)
            }
    }
}
