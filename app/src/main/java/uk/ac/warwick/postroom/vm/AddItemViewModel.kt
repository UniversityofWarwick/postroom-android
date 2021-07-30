package uk.ac.warwick.postroom.vm

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.ac.warwick.postroom.domain.Courier
import uk.ac.warwick.postroom.domain.RecognisedBarcode

class AddItemViewModel : ViewModel() {

    val recipientId: MutableLiveData<String?> by lazy {
        MutableLiveData<String?>()
    }

    val courierId: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val qrId: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val trackingBarcode: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val allCollectedBarcodes: MutableLiveData<Set<RecognisedBarcode>> by lazy {
        MutableLiveData<Set<RecognisedBarcode>>()
    }

    val couriers: MutableLiveData<List<Courier>> by lazy {
        MutableLiveData<List<Courier>>()
    }

    val bitmap: MutableLiveData<Bitmap> by lazy {
        MutableLiveData<Bitmap>()
    }

}
