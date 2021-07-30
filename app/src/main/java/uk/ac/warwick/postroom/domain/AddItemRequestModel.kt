package uk.ac.warwick.postroom.domain

import kotlinx.serialization.Serializable

@Serializable
data class AddItemRequestModel(
    var qrId: String? = null,

    var chosenCourierId: String? = null,

    var trackingBarcode: String? = null,

    var locationBarcode: String? = null,

    var recipientId: String? = null,

    var rts: Boolean = false,

    val sendNotifications: Boolean = false,

    val fromApp: Boolean = true,

    val collectedBarcodes: List<RecognisedBarcode>? = null
)
