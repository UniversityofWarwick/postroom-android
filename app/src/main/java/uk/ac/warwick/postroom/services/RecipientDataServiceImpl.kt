package uk.ac.warwick.postroom.services

import android.content.Context
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.awaitResponse
import com.github.kittinunf.fuel.core.awaitResponseResult
import com.github.kittinunf.fuel.core.awaitResult
import com.github.kittinunf.fuel.coroutines.awaitObject
import com.github.kittinunf.fuel.coroutines.awaitObjectResult
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

    override suspend fun getUniversityIdToUuidMap(): Result<Map<String, String>> {
        val fuelResponse = Fuel.get(
            "${providesBaseUrl.getBaseUrl()}api/app/uni-ids"
        ).useHttpCache(false).withSscAuth(sscPersistenceService.getSsc()!!)
            .awaitObjectResult<JsonObject>(kotlinxDeserializerOf())

        val fuelResult = fuelResponse.component2()
        if (fuelResult != null) {
            return Result.failure(fuelResult.exception)
        }
        return Result.success(fuelResponse.get().map { it.key to it.value.toString() }.toMap())
    }


    override suspend fun getRoomToUuidMap(): Result<Map<String, String>> {
        val fuelResponse = Fuel.get(
            "${providesBaseUrl.getBaseUrl()}api/app/rooms"
        ).useHttpCache(false).withSscAuth(sscPersistenceService.getSsc()!!).useHttpCache(true)
            .awaitObjectResult<JsonObject>(kotlinxDeserializerOf())
        val fuelResult = fuelResponse.component2()
        if (fuelResult != null) {
            return Result.failure(fuelResult.exception)
        }
        return Result.success(fuelResponse.get().map { it.key to it.value.toString() }.toMap())
    }
}