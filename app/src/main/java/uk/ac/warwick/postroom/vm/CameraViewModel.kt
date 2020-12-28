package uk.ac.warwick.postroom.vm

import android.graphics.Rect
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.ac.warwick.postroom.services.CachedRecipientDataService

class CameraViewModel : ViewModel() {

    val uniId: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val room: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
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

    val trackingBarcode: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val trackingFormat: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val uniIds: MutableLiveData<Map<String, String>> by lazy {
        MutableLiveData<Map<String, String>>()
    }

    val rooms: MutableLiveData<Map<String, String>> by lazy {
        MutableLiveData<Map<String, String>>()
    }

    fun cacheData(cachedRecipientDataService: CachedRecipientDataService) {
        viewModelScope.launch {
            cachedRecipientDataService.getUniversityIdToUuidMap { uniIds.postValue(it) }
            cachedRecipientDataService.getRoomToUuidMap { rooms.postValue(it) }
        }
    }
}
