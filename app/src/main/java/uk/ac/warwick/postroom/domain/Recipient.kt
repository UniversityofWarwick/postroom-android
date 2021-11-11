package uk.ac.warwick.postroom.domain

import kotlinx.serialization.Serializable
import uk.ac.warwick.postroom.utils.LdSerializer
import java.time.LocalDate

enum class RecipientSource {
    ManualEntry,
    KineticImport,
    ConferenceImport,
    KineticApplicantImport
}

enum class RecipientType {
    Student, StaffFamily, RLT, CAL, VacationTenant, MRC, AccommodationApplicant,
}

@Serializable
data class Recipient(
    var id: String? = null,
    var source: RecipientSource,
    var type: RecipientType,
    var kineticRoom: String? = null,
    var kineticFloor: Int? = null,
    var room: String? = null,
    var universityId: String,
    var firstName: String,
    var middleName: String? = null,
    var lastName: String,
    var preferredName: String? = null,
    var kineticId: Int? = null,
    var inactive: Boolean = false,
    var accommodationBlock: AccommodationBlock? = null,
    var subAccommodationBlock: AccommodationBlock? = null,
    var email: String? = null,
    @Serializable(with = LdSerializer::class)
    var validFrom: LocalDate? = null,
    @Serializable(with = LdSerializer::class)
    var validTo: LocalDate? = null,
    @Serializable(with = LdSerializer::class)
    var checkedIn: LocalDate? = null,
    @Serializable(with = LdSerializer::class)
    var checkedOut: LocalDate? = null,
    @Serializable(with = LdSerializer::class)
    var selfIsolatingUntil: LocalDate? = null,
    var kxApplicationId: Int? = null,
    var recipientCheckedIn: Boolean? = null
) {
    override fun toString(): String {
        return """$firstName $lastName / $room / $universityId"""
    }

    fun preferredNameOrFullName(): String {
        if (this.preferredName != null) {
            return this.preferredName!!
        }
        return this.firstName + " " + this.lastName
    }

    fun hasDistinctNames(): Boolean = (this.firstName + " " + this.lastName) != this.preferredName
}

@Serializable
data class AutocompleteResponse(
    var data: List<Recipient>? = null
)