package uk.ac.warwick.postroom.domain

import kotlinx.serialization.Serializable

@Serializable
data class PostalHub(
    var id: String? = null,
    var name: String? = null,
    var location: String? = null,
    var displayOrder: Int? = null,
    var mapId: Int? = null,
    var fgColour: String? = null,
    var bgColour: String? = null
)
