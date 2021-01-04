package uk.ac.warwick.postroom.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import uk.ac.warwick.postroom.fuel.withSscAuth

class HomeViewModel : ViewModel() {

    val usercode: MutableLiveData<String?> by lazy {
        MutableLiveData<String?>()
    }

    val photo: MutableLiveData<String?> by lazy {
        MutableLiveData<String?>()
    }

    fun clearUserDetails() {
        usercode.postValue(null)
        photo.postValue(null)
    }

    fun testSscRequest(baseUrl: String, ssc: String) {
        viewModelScope.launch {
            val result = Fuel.get(
                "${baseUrl}api/me"

            ).withSscAuth(ssc).responseObject<BasicUserInformation>(kotlinxDeserializerOf()) {_, _, result ->
                if (result.component2() == null) {
                    usercode.postValue(result.component1()!!.usercode)
                    photo.postValue(result.component1()!!.photoUrl)
                }
            }

        }
    }

    @Serializable
    data class BasicUserInformation(
        val fullName: String,
        val photoUrl: String,
        val dept: String,
        val universityId: String,
        val usercode: String,
        val roles: List<String>
    )

}
