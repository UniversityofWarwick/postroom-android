package uk.ac.warwick.postroom.services

interface SscPersistenceService {
    fun putSsc(ssc: String)
    fun getSsc(): String?
}