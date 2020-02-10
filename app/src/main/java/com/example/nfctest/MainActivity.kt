package com.example.nfctest

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.nfctest.ui.main.MainFragment
import java.io.IOException
import java.nio.charset.Charset


class MainActivity : AppCompatActivity() {
    var mAdapter: NfcAdapter? = null
    var pendingIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commitNow()
        }

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
        )
        findViewById<Button>(R.id.button).setOnClickListener {
            findViewById<TextView>(R.id.startDate).text = ""
            findViewById<TextView>(R.id.endDate).text = ""
            findViewById<TextView>(R.id.title).text = ""
            findViewById<TextView>(R.id.uniId).text = ""
            findViewById<TextView>(R.id.issueNo).text = ""

        }

//        findViewById<Button>(R.id.button).setOnClickListener {
//            val tagDetected = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
//            val tagFilters = arrayOf(tagDetected)
//            val techList = arrayOf(
//                arrayOf(
//                    MifareClassic::class.java.name
//                )
//            )
//            Log.i("Button", "Registered foreground dispatch")
//            Log.i("Button", techList[0][0])
//        }
    }

    override fun onResume() {
        super.onResume();
        val tagDetected = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        val tagFilters = arrayOf(tagDetected)
        val techList = arrayOf(
            arrayOf(
                MifareClassic::class.java.name
            )
        )
        mAdapter?.enableForegroundDispatch(this, pendingIntent, tagFilters, techList);
    }
    override fun onPause(){
        super.onPause();
        mAdapter?.disableForegroundDispatch(this);
    }


    override fun onNewIntent(intent: Intent) {
        Log.i("Intent", "Hit onNewIntent")
        super.onNewIntent(intent) // Tag writing mode
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
                        auth = mfc.authenticateSectorWithKeyA(1, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)
                        if (auth) {
                            bCount = mfc.getBlockCountInSector(1)
                            for (i in 0 until bCount) {
                                bIndex = mfc.sectorToBlock(1)+i
                                data = data.plus(mfc.readBlock(bIndex))
                            }
                            val uniId =
                                data.slice(0..7).toByteArray().toString(Charset.defaultCharset())
                            findViewById<TextView>(R.id.uniId).text = uniId
                            Log.i(
                                "DATA",
                                "University ID (with check digit): "+ uniId
                            )
                            val issueId =
                                data.slice(8..9).toByteArray().toString(Charset.defaultCharset())
                            findViewById<TextView>(R.id.issueNo).text = issueId

                            Log.i(
                                "DATA",
                                "Issue number: "+ issueId
                            )
                            val beginDate =
                                data.slice(10..15).toByteArray().toString(Charset.defaultCharset())
                            findViewById<TextView>(R.id.startDate).text = beginDate

                            Log.i(
                                "DATA",
                                "Begin date: "+ beginDate
                            )

                            val endDate =
                                data.slice(16..21).toByteArray().toString(Charset.defaultCharset())
                            findViewById<TextView>(R.id.endDate).text = endDate

                            Log.i(
                                "DATA",
                                "End date: "+ endDate
                            )

                            val titleChunk = data.slice(23 until data.size).toByteArray()
                            val titleStr =
                                titleChunk.slice(0 until titleChunk.indexOf(0x00)).toByteArray()
                                    .toString(Charset.defaultCharset())
                            findViewById<TextView>(R.id.title).text = titleStr

                            Log.i(
                                "DATA",
                                "Title: "+ titleStr
                            )
                        } else { // Authentication failed - Handle it
                        }
                } catch (e: IOException) {
                    Log.e("BAD", e.localizedMessage)
                }
            }
        }
    }
}
