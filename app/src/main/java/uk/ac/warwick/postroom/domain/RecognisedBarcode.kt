package uk.ac.warwick.postroom.domain

import kotlinx.serialization.Serializable

@Serializable
data class RecognisedBarcode (
    val format: BarcodeFormat?,
    val barcode: String
)