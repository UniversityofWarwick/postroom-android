package uk.ac.warwick.postroom.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import uk.ac.warwick.postroom.POSTROOM_BASE_URL_DEFAULT
import uk.ac.warwick.postroom.R
import javax.inject.Inject

class CustomTabsServiceImpl @Inject constructor(
    @ApplicationContext val applicationContext: Context
) : CustomTabsService {

    override fun getPackageToUse(): String {
        val pm = applicationContext.packageManager
        var packageToUse = "com.android.chrome"

        // Use a representative URL to work out which packages are capable of opening a typical tab
        val activityIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse("https://warwick.ac.uk"))

        // Does the user have a default browser?
        var defaultViewHandlerPackageName: String? = null
        val defaultViewHandlerInfo = pm.resolveActivity(activityIntent, 0)
        if (defaultViewHandlerInfo != null) {
            defaultViewHandlerPackageName = defaultViewHandlerInfo.activityInfo.packageName
        }

        val resolvedActivityList =
            pm.queryIntentActivities(activityIntent, 0)
        val packagesSupportingCustomTabs: MutableList<String> =
            ArrayList()
        for (info in resolvedActivityList) {
            val serviceIntent = Intent()
            serviceIntent.action =
                androidx.browser.customtabs.CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
            serviceIntent.setPackage(info.activityInfo.packageName)
            // Check if this package also resolves the Custom Tabs service.
            if (pm.resolveService(serviceIntent, 0) != null) { // Great, add it to the list
                packagesSupportingCustomTabs.add(info.activityInfo.packageName)
            }
        }

        if (packagesSupportingCustomTabs.isNotEmpty()) {
            // prefer the user's default browser if it supports custom tabs
            packageToUse =
                if (packagesSupportingCustomTabs.contains(defaultViewHandlerPackageName)) {
                    defaultViewHandlerPackageName!!
                } else {
                    // arbitrarily pick the first one
                    packagesSupportingCustomTabs[0]
                }
        }
        return packageToUse
    }

    override fun getBaseUrl(): String =
        PreferenceManager.getDefaultSharedPreferences(applicationContext).getString(
            applicationContext.getString(R.string.instance_url_pref_id),
            POSTROOM_BASE_URL_DEFAULT
        )!!.replace("^(.*[^/])$".toRegex()) { it.groupValues[0] +"/" }
}