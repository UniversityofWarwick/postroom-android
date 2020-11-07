package uk.ac.warwick.postroom.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.json.responseJson
import kotlinx.coroutines.launch
import uk.ac.warwick.postroom.fuel.withSscAuth

class HomeViewModel : ViewModel() {

    val usercode: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    fun testSscRequest(baseUrl: String, ssc: String) {
        viewModelScope.launch {
            val result = Fuel.get(
                "${baseUrl}begin-app-link/user/"
            ).withSscAuth(ssc).responseJson {req, resp, result ->
                if (result.component2() == null) {
                    usercode.postValue(result.component1()!!.obj().getString("usercode"))
                }
            }

        }
    }

}
