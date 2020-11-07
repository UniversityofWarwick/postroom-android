package uk.ac.warwick.postroom.ui.main

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.*
import androidx.core.content.ContextCompat.getColor
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import uk.ac.warwick.postroom.PROCESS_INCOMING_ROUTE
import uk.ac.warwick.postroom.R
import uk.ac.warwick.postroom.SSO_PROD_AUTHORITY
import uk.ac.warwick.postroom.services.CustomTabsService
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    @Inject
    lateinit var customTabsService: CustomTabsService

    private var customTabsSession: CustomTabsSession? = null
    private var tabsConnection: CustomTabsServiceConnection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onStart() {
        super.onStart()
        initCustomTabs()
    }

    override fun onStop() {
        super.onStop()
        deinitCustomTabs()
    }

    fun initCustomTabs() {
        tabsConnection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(
                name: ComponentName,
                client: CustomTabsClient
            ) {
                Log.i(uk.ac.warwick.postroom.TAG, "Custom Tabs service connected")
                client.warmup(0)
                customTabsSession = client.newSession(CustomTabsCallback())
            }

            override fun onServiceDisconnected(name: ComponentName) {
                Log.e(uk.ac.warwick.postroom.TAG, "Custom Tabs service disconnected/crashed")
                customTabsSession = null
            }
        }

        CustomTabsClient.bindCustomTabsService(
            this,
            customTabsService.getPackageToUse(),
            tabsConnection as CustomTabsServiceConnection
        )
    }

    fun deinitCustomTabs() {
        Log.i(uk.ac.warwick.postroom.TAG, "Disconnecting custom tabs")
        if (tabsConnection != null) {
            unbindService(tabsConnection!!)
            tabsConnection = null
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }

        override fun onPreferenceTreeClick(
            preference: Preference
        ): Boolean {
            when (preference.key) {
                "link" -> {

                    val settingsActivity = activity as SettingsActivity
                    val intent = getCustomTabsIntent(settingsActivity)
                    intent.launchUrl(
                        requireContext(),
                        Uri.parse(settingsActivity.customTabsService.getBaseUrl() + "begin-app-link/")
                    )
                }

                "logout" -> {
                    val settingsActivity = activity as SettingsActivity
                    val intent = getCustomTabsIntent(settingsActivity)
                    val builder = Uri.Builder()
                    builder.scheme("https").authority(SSO_PROD_AUTHORITY).appendPath("origin")
                        .appendPath("logout")
                        .appendQueryParameter("target", settingsActivity.customTabsService.getBaseUrl() + PROCESS_INCOMING_ROUTE)

                    intent.launchUrl(
                        requireContext(),
                        builder.build()
                    )
                }
            }
            return super.onPreferenceTreeClick(preference)
        }

        private fun getCustomTabsIntent(settingsActivity: SettingsActivity): CustomTabsIntent {
            return CustomTabsIntent.Builder(settingsActivity.customTabsSession)
                .setToolbarColor(
                    getColor(requireContext(), R.color.colorPrimaryDark)
                )
                .setStartAnimations(
                    requireContext(),
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
                .setExitAnimations(
                    requireContext(),
                    android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right
                )
                .build()
        }


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                super.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}