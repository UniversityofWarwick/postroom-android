package uk.ac.warwick.postroom.activities

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.*
import androidx.core.content.ContextCompat.getColor
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import uk.ac.warwick.postroom.R
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

    fun onEventStarted() {
        // 
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
                Log.i(TAG, "Custom Tabs service connected")
                client.warmup(0)
                customTabsSession = client.newSession(CustomTabsCallback())

                if (this@SettingsActivity.intent.getBooleanExtra("link", false)) {
                    val intent = getCustomTabsIntent(this@SettingsActivity)
                    intent.launchUrl(
                        this@SettingsActivity,
                        Uri.parse(this@SettingsActivity.customTabsService.getBaseUrl() + "begin-app-link/")
                    )
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {
                Log.e(TAG, "Custom Tabs service disconnected/crashed")
                customTabsSession = null
            }
        }

        CustomTabsClient.bindCustomTabsService(
            this,
            customTabsService.getPackageToUse(preferChrome = true),
            tabsConnection as CustomTabsServiceConnection
        )
    }

    fun deinitCustomTabs() {
        Log.i(TAG, "Disconnecting custom tabs")
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
                    val intent = settingsActivity.getCustomTabsIntent(settingsActivity)
                    intent.launchUrl(
                        requireContext(),
                        Uri.parse(settingsActivity.customTabsService.getBaseUrl() + "begin-app-link/")
                    )
                }

                "logout" -> {
                    val settingsActivity = activity as SettingsActivity
                    val intent = settingsActivity.getCustomTabsIntent(settingsActivity)
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

    fun getCustomTabsIntent(settingsActivity: SettingsActivity): CustomTabsIntent {
        return CustomTabsIntent.Builder(settingsActivity.customTabsSession)
            .setToolbarColor(
                getColor(settingsActivity, R.color.colorPrimaryDark)
            )
            .setStartAnimations(
                settingsActivity,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
            .setExitAnimations(
                settingsActivity,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
            .build()
    }
}