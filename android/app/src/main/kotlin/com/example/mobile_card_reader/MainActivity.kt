package com.example.mobile_card_reader

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import io.flutter.embedding.android.FlutterActivity

class MainActivity: FlutterActivity() {

    private var adapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        print("ON CREATE")

        super.onCreate(savedInstanceState, persistentState)

        initNFC()

        val intent = intent
        val action = intent.action
        val type = intent.type


        Log.d("Intent Type", type)
        Log.d("Intent Action", action)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val intent = intent
        val action = intent.action
        val type = intent.type

        Log.d("Intent Type", type)
        Log.d("Intent Action", action)
    }

    private fun initNFC() {
        val nfcManager = getSystemService(Context.NFC_SERVICE) as NfcManager
        adapter = nfcManager.defaultAdapter
    }

    override fun onResume() {
        super.onResume()
        enableNfcForegroundDispatch()
    }

    private fun enableNfcForegroundDispatch() {
        try {
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val nfcPendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
            adapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null)
        } catch (ex: IllegalStateException) {
            Log.e("OPA", "Error enabling NFC foreground dispatch", ex)
        }
    }


    override fun onPause() {
        disableNfcForegroundDispatch()
        super.onPause()
    }

    private fun disableNfcForegroundDispatch() {
        try {
            adapter?.disableForegroundDispatch(this)
        } catch (ex: IllegalStateException) {
            Log.e("OPA", "Error disabling NFC foreground dispatch", ex)
        }
    }
}
