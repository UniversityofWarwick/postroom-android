package uk.ac.warwick.postroom.services

interface CustomTabsService {
    fun getPackageToUse(preferChrome: Boolean): String
    fun getBaseUrl(): String
}