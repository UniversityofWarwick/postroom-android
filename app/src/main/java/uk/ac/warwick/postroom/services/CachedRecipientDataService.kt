package uk.ac.warwick.postroom.services

interface CachedRecipientDataService {
    fun getUniversityIdToUuidMap(callback: (Map<String, String>) -> Unit)
    fun getRoomToUuidMap(callback: (Map<String, String>) -> Unit)
}