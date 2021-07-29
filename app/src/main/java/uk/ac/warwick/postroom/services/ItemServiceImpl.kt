package uk.ac.warwick.postroom.services

import android.content.Context
import android.graphics.Bitmap
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.awaitUnit
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.coroutines.awaitObjectResult
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import com.github.kittinunf.result.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import uk.ac.warwick.postroom.domain.AddItemRequestModel
import uk.ac.warwick.postroom.domain.ItemResult
import uk.ac.warwick.postroom.fuel.withSscAuth
import uk.ac.warwick.postroom.fuel.withSscAuthAndCsrfToken
import uk.ac.warwick.postroom.vm.AddItemViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject

class ItemServiceImpl @Inject constructor(
    @ApplicationContext val applicationContext: Context,
    val providesBaseUrl: ProvidesBaseUrl,
    val sscPersistenceService: SscPersistenceService
) : ItemService {
    // Run in an IO dispatcher context always
    override suspend fun uploadImageForItem(id: UUID, image: Bitmap) {
        val cacheDir = applicationContext.cacheDir
        val file = File.createTempFile(id.toString(), ".png", cacheDir);
        image.compress(Bitmap.CompressFormat.PNG, 90, FileOutputStream(file))
        val fdp = FileDataPart(file, "file")
        Fuel.upload(
            "${providesBaseUrl.getBaseUrl()}api/images/$id/upload"
        ).add(fdp).withSscAuth(sscPersistenceService.getSsc()!!).awaitUnit()
    }

    override suspend fun addItem(model: AddItemViewModel): Result<ItemResult, FuelError> {
        val json = Json.Default
        val courierId = model.courierId
        val body = json.encodeToString(
            AddItemRequestModel(
                qrId = model.qrId.value,
                chosenCourierId = courierId.value,
                trackingBarcode = model.trackingBarcode.value,
                recipientId = model.recipientId.value,
                sendNotifications = false,
                rts = false
            )
        )

        val value = UUID.randomUUID().toString()
        return Fuel.post("${providesBaseUrl.getBaseUrl()}process-incoming/").withSscAuthAndCsrfToken(sscPersistenceService.getSsc()!!, value)
            .jsonBody(body)
            .header("X-XSRF-TOKEN", value) // hacky!
            .awaitObjectResult(kotlinxDeserializerOf(Json {
                ignoreUnknownKeys = true
            }))
    }

}