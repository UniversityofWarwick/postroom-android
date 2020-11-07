package uk.ac.warwick.postroom

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Build
import android.os.Bundle
import android.provider.Browser
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.*
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import dagger.hilt.android.AndroidEntryPoint
import uk.ac.warwick.postroom.services.CustomTabsService
import uk.ac.warwick.postroom.services.SscPersistenceService
import uk.ac.warwick.postroom.ui.main.SettingsActivity
import uk.ac.warwick.postroom.vm.HomeViewModel
import java.io.IOException
import javax.inject.Inject


const val POSTROOM_BASE_URL_DEFAULT = "https://postroom.warwick.ac.uk/"
const val PROCESS_INCOMING_ROUTE = "process-incoming/"
const val COLLECTION_ROUTE = "process-collection/"
const val AUDITS_ROUTE = "admin/audits/"
const val RTS_SPR_ROUTE = "rts/"
const val MOVE_ITEMS_ROUTE = "move/"
const val RTS_COURIER_ROUTE = "rts/couriers/"
const val SHELF_AUDIT_ROUTE = "admin/shelving-audit/"
const val SSO_PROD_AUTHORITY = "websignon.warwick.ac.uk"

const val TAG = "Postroom"

const val SSC_NAME = "SSO-Postroom-SSC-Domain"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    var mAdapter: NfcAdapter? = null
    var pendingIntent: PendingIntent? = null

    @Inject lateinit var customTabsService: CustomTabsService
    @Inject lateinit var sscPersistenceService: SscPersistenceService

    private var customTabsSession: CustomTabsSession? = null
    private var tabsConnection: CustomTabsServiceConnection? = null
    private var universityId: String? = null

    private val model: HomeViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        mAdapter = NfcAdapter.getDefaultAdapter(this)
        pendingIntent = PendingIntent.getActivity(
            applicationContext, 0,
            Intent(
                applicationContext,
                applicationContext.javaClass
            ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
        )

        findViewById<Button>(R.id.process_incoming_parcels).setOnClickListener {
            goToUrl(getBaseUrl() + PROCESS_INCOMING_ROUTE)
        }

        findViewById<Button>(R.id.manual_item_collection).setOnClickListener {
            goToUrl(getBaseUrl() + COLLECTION_ROUTE)
        }

        findViewById<Button>(R.id.view_activity).setOnClickListener {
            goToUrl(getBaseUrl() + AUDITS_ROUTE)
        }

        findViewById<Button>(R.id.shelf_audit).setOnClickListener {
            goToUrl(getBaseUrl() + SHELF_AUDIT_ROUTE)
        }

        findViewById<Button>(R.id.rts).setOnClickListener {
            handleRts()
        }

        findViewById<Button>(R.id.move_items).setOnClickListener {
            handleMoveItems()
        }

        model.usercode.observe(this, Observer<String> { newUsercode ->
            Log.i(TAG, "New usercode")
            Toast.makeText(this, "Logged in as $newUsercode", Toast.LENGTH_SHORT).show()
        })
    }

    private fun handleRts() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Which statement best describes what you want to do?")
        builder.setItems(
            arrayOf<CharSequence>(
                getString(R.string.i_work_in_the_spr_rts),
                getString(R.string.i_am_a_courier_rts)
            )
        ) { _, which ->
            when (which) {
                0 -> goToUrl(getBaseUrl() + RTS_SPR_ROUTE)
                1 -> goToUrl(getBaseUrl() + RTS_COURIER_ROUTE)
            }
        }
        builder.setNegativeButton("Cancel") { _: DialogInterface, _: Int -> }
        builder.create().show()
    }



    private fun handleMoveItems() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Which statement best describes what you want to do?")
        builder.setItems(
            arrayOf<CharSequence>(
                "I am processing received items at a postal hub; I want to notify students",
                "I am re-organising items, I don't want notifications"
            )
        ) { _, which ->
            when (which) {
                0 -> goToUrl(getBaseUrl() + MOVE_ITEMS_ROUTE + "?hub")
                1 -> goToUrl(getBaseUrl() + MOVE_ITEMS_ROUTE)
            }
        }
        builder.setNegativeButton("Cancel") { _: DialogInterface, _: Int -> }
        builder.create().show()
    }

    private fun goToUrl(uriString: String) {
        val intent = buildCustomTabsIntent()

        try {
            val uri = Uri.parse(uriString)
            intent.launchUrl(
                this,
                uri
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open custom tab", e)
        }
    }

    private fun switchUser() {
        val intent = buildCustomTabsIntent()
        val builder = Uri.Builder()
        builder.scheme("https").authority(SSO_PROD_AUTHORITY).appendPath("origin")
            .appendPath("logout")
            .appendQueryParameter("target", getBaseUrl() + PROCESS_INCOMING_ROUTE)
        try {
            intent.launchUrl(
                this,
                builder.build()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open custom tab", e)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.toolbar, menu)
        return true // to show the menu
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settingsBtn -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.cameraBtn -> {
                startActivity(Intent(this, CameraActivity::class.java))
                true
            }
            R.id.switchUserBtn -> {
                switchUser()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun buildCustomTabsIntent(): CustomTabsIntent {
        val intent = CustomTabsIntent.Builder(customTabsSession)
            .setToolbarColor(
                getColor(R.color.colorPrimaryDark)
            )
            .setStartAnimations(this, R.anim.slide_in_right, R.anim.slide_out_left)
            .setExitAnimations(
                this,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
            .build()

        val headers = Bundle()
        // hilarious abuse of HTTP but I have my reasons for doing this
        headers.putString("Accept-Language", "POSTROOM")
        intent.intent.putExtra(Browser.EXTRA_HEADERS, headers)
        return intent
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



        CustomTabsClient.bindCustomTabsService(
            this,
            customTabsService.getPackageToUse(),
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
            val uri = Uri.parse(getBaseUrl() + COLLECTION_ROUTE + universityId)
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
        mAdapter?.disableForegroundDispatch(this)
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
        if (intent.data?.path == "/app-link/ssc" && intent.data?.getQueryParameter("value") != null) {
            val ssc = intent.data?.getQueryParameter("value")
            sscPersistenceService.putSsc(ssc!!)
            model.testSscRequest(getBaseUrl(), ssc)
            return
        }

        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            val detectedTag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            if (detectedTag != null) {
                val mfc = MifareClassic.get(detectedTag)
                var data = ByteArray(0)

                try {
                    mfc.connect()
                    val bCount: Int
                    var bIndex: Int
                    val auth: Boolean = mfc.authenticateSectorWithKeyA(
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
                    mfc.close()
                } catch (e: IOException) {
                    Log.e(TAG, "IOException")
                }
            }

        }
    }

    private fun getBaseUrl(): String =
        getDefaultSharedPreferences(this).getString(
            getString(R.string.instance_url_pref_id),
            POSTROOM_BASE_URL_DEFAULT
        )!!.replace("^(.*[^/])$".toRegex()) { it.groupValues[0].toString()+"/" }


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
