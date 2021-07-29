package uk.ac.warwick.postroom.domain

import kotlinx.serialization.Serializable

@Serializable
data class ItemAdditionError(
    val error: String
)
