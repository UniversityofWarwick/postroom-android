package uk.ac.warwick.postroom.services

import android.content.Context
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.JsonObject
import uk.ac.warwick.postroom.fuel.withSscAuth
import java.lang.IllegalStateException
import javax.inject.Inject

class RecipientDataServiceImpl @Inject constructor(
    @ApplicationContext val applicationContext: Context,
    val providesBaseUrl: ProvidesBaseUrl,
    val sscPersistenceService: SscPersistenceService
) : RecipientDataService {

    override fun getUniversityIdToUuidMap(callback: (Map<String, String>) -> Unit) {
        Fuel.get(
            "${providesBaseUrl.getBaseUrl()}api/app/uni-ids"
        ).useHttpCache(false).withSscAuth(sscPersistenceService.getSsc()!!)
            .responseObject<JsonObject>(kotlinxDeserializerOf()) { _, _, result ->
                if (result.component2() != null) {
                    throw IllegalStateException("Failed to fetch rooms")
                }
                callback(result.get().map { it.key to it.value.toString() }.toMap())
            }
    }

    override fun getRoomToUuidMap(callback: (Map<String, String>) -> Unit) {
        Fuel.get(
            "${providesBaseUrl.getBaseUrl()}api/app/rooms"
        ).useHttpCache(false).withSscAuth(sscPersistenceService.getSsc()!!).useHttpCache(true)
            .responseObject<JsonObject>(kotlinxDeserializerOf()) { _, _, result ->
                if (result.component2() != null) {
                    throw IllegalStateException("Failed to fetch rooms")
                }
                callback(result.get().map { it.key to it.value.toString() }.toMap())

            }
    }
}