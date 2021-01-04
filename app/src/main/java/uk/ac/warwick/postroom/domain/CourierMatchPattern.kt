package uk.ac.warwick.postroom.domain

import kotlinx.serialization.Serializable
import uk.ac.warwick.postroom.utils.OdtSerializer
import java.time.OffsetDateTime

@Serializable
data class CourierMatchPattern(
    var id: String? = null,

    var pattern: String,

    var reject: Boolean = false,

    var description: String? = null,

    var barcodeFormatHint: BarcodeFormat? = null,

    @Serializable(with = OdtSerializer::class)
    var createdAt: OffsetDateTime? = null,

    var courier: Courier? = null
) {
    fun matchesBarcode(recognisedBarcode: RecognisedBarcode): Boolean {
        return pattern.toRegex()
            .matchEntire(recognisedBarcode.barcode) != null && (barcodeFormatHint == null || recognisedBarcode.format == barcodeFormatHint)
    }
}

enum class BarcodeFormat {
    Code128,
    Code39,
    Code93,
    Codabar,
    DataMatrix,
    Ean13,
    Ean8,
    Itf,
    Qr,
    UpcA,
    UpcE,
    Pdf417,
    Aztec
}
