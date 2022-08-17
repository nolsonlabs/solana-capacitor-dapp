package com.nolson.plugins.solanawalletadaptor

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.GuardedBy
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import org.json.JSONArray

@CapacitorPlugin(name = "SolanaWalletAdaptor")
class SolanaWalletAdaptorPlugin : Plugin() {

    private val application = Application();
    private val implementation = SolanaWalletAdaptor(application)

    var authToken: String = "";
    var authorized: Boolean = false;

    @PluginMethod
    fun echo(call: PluginCall) {
        val value = call.getString("value")
        val ret = JSObject()
        call.resolve(ret)
    }


    @PluginMethod
    fun isPackageInstalled(call: PluginCall) {
        val packageName = call.getString("packages")
        val ret = JSObject()
        ret.put(
            "installed",
            implementation.installedApps(activity.packageManager)
        )
        call.resolve(ret)
    }

    @PluginMethod
    fun installedApps(call: PluginCall) {
        val ret = JSObject()
        val data = implementation.installedApps(activity.packageManager)

        ret.put(
            "installed",
            data
        )
        call.resolve(ret)
    }

    @PluginMethod
    fun checkIsWalletEndpointAvailable(call: PluginCall) {
        val ret = JSObject()
        ret.put(
            "endpointAvailable",
            implementation.checkIsWalletEndpointAvailable(activity.packageManager)
        )
        call.resolve(ret)
    }

    @PluginMethod
    fun getCapabilities(call: PluginCall) {
        val ret = JSObject()
        // Keep alive until done
        call.setKeepAlive(true)

        implementation.getCapabilities(intentSender)
        ret.put(
            "capabilitiesRequested",
            true
        )
        call.resolve(ret)
    }

    @PluginMethod
    fun authorize(call: PluginCall) {
        implementation.authorize(intentSender, call, "plugin")
    }

    @PluginMethod
    fun reauthorize(call: PluginCall) {
        val authToken = call.getString("authToken");
        Log.d(TAG, "CALL CONTENTS")
        Log.d(TAG, call.toString())
        implementation.reauthorize(call, intentSender, authToken.toString(), "plugin");
    }

    @PluginMethod
    fun deauthorize(call: PluginCall) {
        val authToken = call.getString("authToken");
        implementation.deauthorize(call, intentSender, authToken.toString())
    }

    @PluginMethod
    fun signTransactions(call: PluginCall) {
        val count = call.getInt("count")!!.toInt()
        val authToken = call.getString("authToken")
        val publicKey = call.getString("publicKey").toString()
        val charset = Charsets.UTF_8
        val publicKeyByteArr = publicKey.toByteArray(charset)
        Log.d(TAG, publicKeyByteArr.toString())
        implementation.signTransactions(call, intentSender, count, authToken.toString(), publicKeyByteArr)
    }

    @PluginMethod
    fun authorizeAndSignTransactions(call: PluginCall) {
        val count = call.getInt("count")!!.toInt()
        val authToken = call.getString("authToken")
        val publicKey = call.getString("publicKey").toString()
        val charset = Charsets.UTF_8
        val publicKeyByteArr = publicKey.toByteArray(charset)
        Log.d(TAG, publicKeyByteArr.toString())
        implementation.authorizeAndSignTransactions(call, intentSender)
    }

    @PluginMethod
    fun signMessages(call: PluginCall) {
        val count = call.getInt("count")!!.toInt()
        val authToken = call.getString("authToken").toString()
        implementation.signMessages(call, intentSender, count, authToken)
    }

    @PluginMethod
    fun signAndSendTransactions(call: PluginCall) {
        val count = call.getInt("count")!!.toInt()
        val authToken = call.getString("authToken").toString()
        implementation.signAndSendTransactions(call, intentSender, count, authToken)
    }

    @PluginMethod
    fun requestAirdrop(call: PluginCall) {
        val authToken = call.getString("authToken").toString()
        implementation.requestAirdrop(call, intentSender, authToken)
    }

    private val intentSender = object : SolanaWalletAdaptor.StartActivityForResultSender {
        @GuardedBy("this")
        private var callback: (() -> Unit)? = null

        override fun startActivityForResult(
            intent: Intent,
            onActivityCompleteCallback: () -> Unit
        ) {
            try {
            synchronized(this) {
                check(callback == null) { "Received an activity start request while another is pending" }
                callback = onActivityCompleteCallback
            }
                activity.startActivityForResult(intent, WALLET_ACTIVITY_REQUEST_CODE)
                // Hack to fix lifecycle issue
                Thread.sleep(750);
                onActivityComplete()
            } catch (e: Exception) {
                Log.d(TAG, e.toString())
            }

        }

        fun onActivityComplete() {
            synchronized(this) {
                callback?.let { it() }
                callback = null
            }
        }

    }

    companion object {
        private val TAG = SolanaWalletAdaptor::class.simpleName
        private const val LOCAL_ASSOCIATION_START_TIMEOUT_MS = 60000L // LocalAssociationScenario.start() has a shorter timeout; this is just a backup safety measure
        private const val LOCAL_ASSOCIATION_CLOSE_TIMEOUT_MS = 5000L
        private const val LOCAL_ASSOCIATION_CANCEL_AFTER_WALLET_CLOSED_TIMEOUT_MS = 5000L
        private val TESTNET_RPC_URI = Uri.parse("https://api.testnet.solana.com")
        private const val WALLET_ACTIVITY_REQUEST_CODE = 1234
    }

}