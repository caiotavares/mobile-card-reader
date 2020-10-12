package com.example.mobile_card_reader

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.os.PersistableBundle
import android.os.StrictMode
import android.util.Log
import android.widget.Toast
import com.google.common.io.BaseEncoding
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody


class MainActivity : FlutterActivity() {

    private val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var tag: Tag? = null
    private val CHANNEL = "app.nfc.data"

    private val select = "00A4040007A0000000041010"
    private val resetPinTryCounter = "8424000008"
    private val getProcessingOptions = "80A80000028300"
    private val generateAc = "80AE8000420000000013370000000013370986000000000009861504280030901B6A2300001EABC126F85499760000000000000000000000000000000000000000000000000000"

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        initNFC()
    }

    override fun onResume() {
        super.onResume()
        enableForegroundDispatch()
    }

    override fun onPause() {
        disableForegroundDispatch()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readNFC(intent)
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        initNFC()

        // Migrate this to the plugin
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "getNfcData") {
                if (tag == null) {
                    result.success("NADA")
                } else {
                    result.success(tag)
                }
            }
        }
    }

    private fun enableForegroundDispatch() {
        pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
        if (nfcAdapter != null) {
            nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    private fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(this);
    }

    private fun initNFC() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show()
            finish()
            return
        } else {
            Toast.makeText(this, "NFC is avaiable", Toast.LENGTH_LONG).show()
        }
    }

    private fun readNFC(intent: Intent) {
        StrictMode.setThreadPolicy(policy) // This enables NETWORK ON MAIN THREAD (for testing purposes)
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            Toast.makeText(this, "NFC READ!", Toast.LENGTH_LONG).show()
            val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            interact(tag)
        }
    }

    private fun interact(tag: Tag) {
        val isoTag = IsoDep.get(tag)
        isoTag.connect()
        sendApdu(isoTag, select)
        sendApdu(isoTag, getProcessingOptions)
        val ac = sendApdu(isoTag, generateAc)
        val mac = getMac(ac)
        sendApdu(isoTag, resetPinTryCounter + mac)
        isoTag.close()
    }

    private fun getMac(ac: String): String {
        val json = "application/json; charset=utf-8".toMediaType()
        val client = OkHttpClient()
        val body = "{\"ac\": \"$ac\"}".toRequestBody(json)
        val request = Request.Builder()
                .url("http:192.168.1.16:8024/api/mac")
                .post(body)
                .build()

        Log.d("OUT-REQUEST: ", body.toString())
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        Log.d("OUT-RESPONSE: ", responseBody)
        return responseBody!!
    }

    private fun sendApdu(tag: IsoDep, apdu: String): String {
        Log.d("C-APDU -> ", apdu)
        val response = tag.transceive(BaseEncoding.base16().decode(apdu))
        Log.d("-> R-APDU ", BaseEncoding.base16().encode(response))
        return BaseEncoding.base16().encode(response)
    }
}
