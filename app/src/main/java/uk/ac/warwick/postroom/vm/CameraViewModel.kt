package uk.ac.warwick.postroom.vm

import android.graphics.Rect
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CameraViewModel : ViewModel() {

    val uniId: MutableLiveData<String> by lazy {
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
}
