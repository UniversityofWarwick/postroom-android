package uk.ac.warwick.postroom.domain

import kotlinx.serialization.Serializable

@Serializable
data class ItemResult(
    var recipient: Recipient?,
    var id: String? = null,
    var status: String? = null,
)
