package uk.ac.warwick.postroom.services

interface RecipientDataService {
    suspend fun getUniversityIdToUuidMap(): Result<Map<String, String>>
    suspend fun getRoomToUuidMap(): Result<Map<String, String>>
}