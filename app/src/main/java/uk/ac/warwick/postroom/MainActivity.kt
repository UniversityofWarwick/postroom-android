package uk.ac.warwick.postroom

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.*
import java.io.IOException

const val POSTROOM_BASE_URL = "https://postroom-dev.warwick.ac.uk/"
const val PROCESS_INCOMING_ROUTE = "process-incoming/"
const val COLLECTION_ROUTE = "process-collection/"

private const val TAG = "Postroom"

class MainActivity : AppCompatActivity() {
    var mAdapter: NfcAdapter? = null
    var pendingIntent: PendingIntent? = null

    private var customTabsSession: CustomTabsSession? = null
    private var tabsConnection: CustomTabsServiceConnection? = null
    private var universityId: String? = null

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        mAdapter = NfcAdapter.getDefaultAdapter(this)
        pendingIntent = PendingIntent.getActivity(
            applicationContext, 0,
            Intent(applicationContext, applicationContext.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
        )

        findViewById<Button>(R.id.process_incoming_parcels).setOnClickListener {
            val intent = buildCustomTabsIntent()

            try {
                val uri = Uri.parse(POSTROOM_BASE_URL+PROCESS_INCOMING_ROUTE)
                intent.launchUrl(
                    this,
                    uri
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open custom tab", e)
            }
        }

        findViewById<Button>(R.id.manual_item_collection).setOnClickListener {
            val intent = buildCustomTabsIntent()

            try {
                val uri = Uri.parse(POSTROOM_BASE_URL+ COLLECTION_ROUTE)
                intent.launchUrl(
                    this,
                    uri
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open custom tab", e)
            }
        }
    }

    private fun buildCustomTabsIntent(): CustomTabsIntent {
        return CustomTabsIntent.Builder(customTabsSession)
            .setToolbarColor(
                this.titleColor
            )
            .setStartAnimations(this, R.anim.slide_in_right, R.anim.slide_out_left)
            .setExitAnimations(
                this,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
            .build()
    }


    fun initCustomTabs() {
        Log.i(TAG, "Connecting to custom tabs")
        tabsConnection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(
                name: ComponentName,
                client: CustomTabsClient
            ) {
                Log.i(TAG, "Custom Tabs service connected")
                client.warmup(0)
                customTabsSession = client.newSession(CustomTabsCallback())
                if (universityId != null) {
                    val customTabIntent = buildCustomTabsIntent()
                    openCollectionCustomTab(customTabIntent)
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {
                Log.e(TAG, "Custom Tabs service disconnected/crashed")
                customTabsSession = null
            }
        }

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
            serviceIntent.action = CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
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

        CustomTabsClient.bindCustomTabsService(
            this,
            packageToUse,
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

    override fun onResume() {
        super.onResume()
        if (intent != null) {
            handleIntent(intent)
        }
        if (universityId != null && tabsConnection != null) {
            val customTabIntent = buildCustomTabsIntent()
            openCollectionCustomTab(customTabIntent)
        } else if (universityId != null) {
            initCustomTabs()
        }
        val tagDetected = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        val tagFilters = arrayOf(tagDetected)
        val techList = arrayOf(
            arrayOf(
                MifareClassic::class.java.name
            )
        )
        mAdapter?.enableForegroundDispatch(this, pendingIntent, tagFilters, techList)

    }

    private fun openCollectionCustomTab(customTabIntent: CustomTabsIntent) {
        try {
            Log.i(TAG, "Opening intent from onResume")
            val uri = Uri.parse(POSTROOM_BASE_URL + COLLECTION_ROUTE + universityId)
            universityId = null
            customTabIntent.launchUrl(
                this@MainActivity,
                uri
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open custom tab", e)
            Toast.makeText(this, "Failed to open custom tab", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        mAdapter?.disableForegroundDispatch(this);
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        initCustomTabs()
    }

    override fun onStop() {
        super.onStop()
        deinitCustomTabs()
    }

    override fun onNewIntent(intent: Intent) {
        Log.i("Intent", "Hit onNewIntent")
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            val detectedTag: Tag? = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (detectedTag != null) {
                val mfc = MifareClassic.get(detectedTag)
                var data: ByteArray = ByteArray(0)

                try {
                    mfc.connect()
                    var auth = false
                    val cardData: String? = null
                    mfc.sectorCount
                    var bCount = 0
                    var bIndex = 0
                    auth = mfc.authenticateSectorWithKeyA(
                        1,
                        MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY
                    )
                    if (auth) {
                        bCount = mfc.getBlockCountInSector(1)
                        for (i in 0 until bCount) {
                            bIndex = mfc.sectorToBlock(1) + i
                            data = data.plus(mfc.readBlock(bIndex))
                        }
                        readMifareData(data)


                    } else { // Authentication failed - Handle it
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "IOException")
                }
            }

        }
    }

    private fun readMifareData(data: ByteArray) {
        val uniId =
            String(data.slice(0..6).toByteArray())
        Log.i(
            TAG,
            "University ID (without check digit): $uniId"
        )
        universityId = uniId
        val issueId =
            String(data.slice(8..9).toByteArray())

        Log.i(
            TAG,
            "Issue number: $issueId"
        )
        val beginDate =
            String(data.slice(10..15).toByteArray())

        Log.i(
            TAG,
            "Begin date: $beginDate"
        )

        val endDate =
            String(data.slice(16..21).toByteArray())

        Log.i(
            TAG,
            "End date: $endDate"
        )

        val titleChunk = data.slice(23 until data.size).toByteArray()
        val titleStr =
            String(titleChunk.slice(0 until titleChunk.indexOf(0x00)).toByteArray())

        Log.i(
            TAG,
            "Title: $titleStr"
        )
    }
}
