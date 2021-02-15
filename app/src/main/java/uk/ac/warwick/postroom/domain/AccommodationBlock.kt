package uk.ac.warwick.postroom.domain

import kotlinx.serialization.Serializable

@Serializable
data class AccommodationBlock(
    var id: String? = null,
    var code: String? = null,
    var name: String? = null,
    var kineticId: Int? = null,
    var kineticSiteId: Int? = null,
    var laCode: String? = null,
    var parent: AccommodationBlock? = null,
    var hub: PostalHub? = null,
    var destinationWebGroup: String? = null,
    var kineticParentId: Int? = null,
    var recipients: List<Recipient>? = null
)