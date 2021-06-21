package uk.ac.warwick.postroom.domain

import kotlinx.serialization.Serializable
import uk.ac.warwick.postroom.utils.LdSerializer
import java.time.LocalDate

@Serializable
data class MiniRecipient(
    var room: String? = null,
    var universityId: String,
    var firstName: String,
    var middleName: String? = null,
    var lastName: String,
    var preferredName: String? = null,
    @Serializable(with = LdSerializer::class)
    var validFrom: LocalDate? = null,
    @Serializable(with = LdSerializer::class)
    var validTo: LocalDate? = null,
    var inactive: Boolean = false,
    var type: RecipientType,
    ) {
    override fun toString(): String {
        return """$firstName $lastName / $room / $universityId"""
    }
}
