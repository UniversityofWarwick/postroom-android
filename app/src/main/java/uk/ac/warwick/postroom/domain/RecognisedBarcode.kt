package uk.ac.warwick.postroom.domain

data class RecognisedBarcode (
    val format: BarcodeFormat?,
    val barcode: String
)