package uk.ac.warwick.postroom.vm

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
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

    val couriers: MutableLiveData<List<Courier>> by lazy {
        MutableLiveData<List<Courier>>()
    }

    val bitmap: MutableLiveData<Bitmap> by lazy {
        MutableLiveData<Bitmap>()
    }

}
