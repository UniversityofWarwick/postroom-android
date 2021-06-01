package uk.ac.warwick.postroom.vm

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.ac.warwick.postroom.activities.SettingsActivity
import uk.ac.warwick.postroom.domain.*
import uk.ac.warwick.postroom.services.CourierMatchService
import uk.ac.warwick.postroom.services.RecipientDataService

class CameraViewModel : ViewModel() {

    val uniId: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val room: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val recipientGuesses: MutableLiveData<Set<RecipientGuess>> by lazy {
        MutableLiveData<Set<RecipientGuess>>()
    }

    val uniIdBoundingBox: MutableLiveData<Rect> by lazy {
        MutableLiveData<Rect>()
    }

    val width: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    val height: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    val barcodes: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    val qrId: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val courierPatterns: MutableLiveData<List<CourierMatchPattern>> by lazy {
        MutableLiveData<List<CourierMatchPattern>>()
    }

    val bestBarcode: MutableLiveData<RecognisedBarcode> by lazy {
        MutableLiveData<RecognisedBarcode>()
    }

    val allCollectedBarcodes: MutableLiveData<Set<RecognisedBarcode>> by lazy {
        MutableLiveData<Set<RecognisedBarcode>>()
    }

    val courierGuess: MutableLiveData<Courier?> by lazy {
        MutableLiveData<Courier?>()
    }

    val uniIds: MutableLiveData<Map<String, String>> by lazy {
        MutableLiveData<Map<String, String>>()
    }

    val rooms: MutableLiveData<Map<String, String>> by lazy {
        MutableLiveData<Map<String, String>>()
    }

    val couriers: MutableLiveData<List<Courier>> by lazy {
        MutableLiveData<List<Courier>>()
    }

    val initialFetchError: MutableLiveData<Throwable> by lazy {
        MutableLiveData<Throwable>()
    }

    fun cacheData(recipientDataService: RecipientDataService, courierMatchService: CourierMatchService) {
        viewModelScope.launch {
            recipientDataService.getUniversityIdToUuidMap().also {
                if (it.isSuccess) {
                    uniIds.postValue(it.getOrThrow())
                } else {
                    initialFetchError.postValue(it.exceptionOrNull()!!)
                }
            }
            recipientDataService.getRoomToUuidMap().also {
                if (it.isSuccess) {
                    rooms.postValue(it.getOrThrow())
                }
            }
            courierMatchService.fetchAllCouriers().also {
                if (it.isSuccess) {
                    couriers.postValue(it.getOrThrow())
                }
            }
            courierMatchService.fetchAllCourierPatterns { courierPatterns.postValue(it) }
        }
    }
}
