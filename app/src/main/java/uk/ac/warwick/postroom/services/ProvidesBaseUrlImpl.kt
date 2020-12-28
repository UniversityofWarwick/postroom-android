package uk.ac.warwick.postroom.services

import android.content.Context
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import uk.ac.warwick.postroom.R
import uk.ac.warwick.postroom.activities.POSTROOM_BASE_URL_DEFAULT
import javax.inject.Inject

class ProvidesBaseUrlImpl @Inject constructor(
    @ApplicationContext val applicationContext: Context
) : ProvidesBaseUrl {

    override fun getBaseUrl(): String {
        return PreferenceManager.getDefaultSharedPreferences(applicationContext).getString(
            applicationContext.getString(R.string.instance_url_pref_id),
            POSTROOM_BASE_URL_DEFAULT
        )!!.replace("^(.*[^/])$".toRegex()) { it.groupValues[0] + "/" }
    }
}