package uk.ac.warwick.postroom.services

import android.graphics.Bitmap
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import uk.ac.warwick.postroom.domain.ItemResult
import uk.ac.warwick.postroom.domain.MiniRecipient
import uk.ac.warwick.postroom.vm.AddItemViewModel
import java.io.ByteArrayOutputStream
import java.util.*

interface ItemService {
    suspend fun uploadImageForItem(id: UUID, image: Bitmap)
    suspend fun addItem(model: AddItemViewModel): Result<ItemResult, FuelError>
}