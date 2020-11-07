package uk.ac.warwick.postroom.services

interface CustomTabsService {
    fun getPackageToUse(): String
    fun getBaseUrl(): String
}