package uk.ac.warwick.postroom.services

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SscPersistenceServiceImpl @Inject constructor(
    @ApplicationContext val application: Context
) : SscPersistenceService {

    override fun putSsc(ssc: String) = secureSharedPreferences.edit().putString("ssc", ssc).apply()
    override fun getSsc(): String? = secureSharedPreferences.getString("ssc", null)

    private val secureSharedPreferences by lazy {
        application.let {
            // create the master key
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "secure_shared_prefs",
                masterKeyAlias,
                it,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
}