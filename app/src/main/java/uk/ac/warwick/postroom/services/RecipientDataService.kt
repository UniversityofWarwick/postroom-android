package uk.ac.warwick.postroom.services

import uk.ac.warwick.postroom.domain.MiniRecipient
import java.util.*

interface RecipientDataService {
    suspend fun getUniversityIdToUuidMap(): Result<Map<String, String>>
    suspend fun getRoomToUuidMap(): Result<Map<String, String>>
    suspend fun getMiniRecipient(id: UUID): Result<MiniRecipient>
}