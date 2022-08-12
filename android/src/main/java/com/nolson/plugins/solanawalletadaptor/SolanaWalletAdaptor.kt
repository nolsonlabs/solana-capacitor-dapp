package com.nolson.plugins.solanawalletadaptor

import com.nolson.plugins.solanawalletadaptor.clientlib.scenario.Scenario
import com.nolson.plugins.solanawalletadaptor.common.util.NotifyOnCompleteFuture
import com.nolson.plugins.solanawalletadaptor.clientlib.protocol.MobileWalletAdapterClient
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.PluginMethod
import com.getcapacitor.PluginCall

import android.app.Application
import androidx.activity.viewModels
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.annotation.GuardedBy
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.nolson.plugins.solanawalletadaptor.clientlib.protocol.JsonRpc20Client
import com.nolson.plugins.solanawalletadaptor.clientlib.scenario.LocalAssociationIntentCreator
import com.nolson.plugins.solanawalletadaptor.clientlib.scenario.LocalAssociationScenario
import com.nolson.plugins.solanawalletadaptor.common.ProtocolContract

import com.solana.mobilewalletadapter.fakedapp.usecase.GetLatestBlockhashUseCase
import com.solana.mobilewalletadapter.fakedapp.usecase.MemoTransactionUseCase
import com.solana.mobilewalletadapter.fakedapp.usecase.RequestAirdropUseCase

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class SolanaWalletAdaptor {

    private val mobileWalletAdapterClientSem = Semaphore(1) // allow only a single MWA connection at a time

    fun echo(value: String): String {
        Log.i("Echo", value)
        return value
    }

    fun listAvailableWallets(): Array<String?> {
        val wallets = arrayOfNulls<String>(1)
        val walletString = "phantom-hardcoded"
        wallets[0] = walletString
        return wallets
    }

    fun isPackageInstalled(packageName: String?, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName!!, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun checkIsWalletEndpointAvailable(packageManager: PackageManager): Boolean {
        return LocalAssociationIntentCreator.isWalletEndpointAvailable(packageManager)
    }

    fun doAuthorize(client: MobileWalletAdapterClient): Boolean {
        var authorized = false
        try {
            val result = client.authorize(
                Uri.parse("https://solana.com"),
                Uri.parse("favicon.ico"),
                "Solana",
                ProtocolContract.CLUSTER_TESTNET
            ).get()
            return true
            // Log.d(TAG, "Authorized: $result")
          //  authorized = true
        } catch (e: ExecutionException) {
            when (val cause = e.cause) {
                is IOException ->
                    return false
                    // Log.e(TAG, "IO error while sending authorize", cause)
                is TimeoutException ->
                    return false
                    //Log.e(TAG, "Timed out while waiting for authorize result", cause)
                is JsonRpc20Client.JsonRpc20RemoteException ->
                    when (cause.code) {
                        ProtocolContract.ERROR_AUTHORIZATION_FAILED ->
                          return false
                        //  Log.e(TAG, "Not authorized", cause)
                        else ->
                            return false
                          //  Log.e(TAG, "Remote exception for authorize", cause)
                    }
                is MobileWalletAdapterClient.InsecureWalletEndpointUriException ->
                    return false
                    // Log.e(TAG, "authorize result contained a non-HTTPS wallet base URI", e)
                is JsonRpc20Client.JsonRpc20Exception ->
                    return false
                    // Log.e(TAG, "JSON-RPC client exception for authorize", cause)
                else -> throw e
            }
        } catch (e: CancellationException) {
            return false
           // Log.e(TAG, "authorize request was cancelled", e)
        } catch (e: InterruptedException) {
            return false
            // Log.e(TAG, "authorize request was interrupted", e)
        }

      //  return authorized
    }

    // GET CAPABILITIES FUNCTION

    fun getCapabilities(sender: StartActivityForResultSender) = viewModelScope.launch {
        val result = localAssociateAndExecute(sender) { client ->
            doGetCapabilities(client)
        }

       // showMessage(if (result != null) R.string.msg_request_succeeded else R.string.msg_request_failed)
    }

    private val intentSender = object : StartActivityForResultSender {
        @GuardedBy("this")
        private var callback: (() -> Unit)? = null

        override fun startActivityForResult(
            intent: Intent,
            onActivityCompleteCallback: () -> Unit
        ) {
            synchronized(this) {
                check(callback == null) { "Received an activity start request while another is pending" }
                callback = onActivityCompleteCallback
            }
            this@SolanaWalletAdaptor.startActivityForResult(intent, WALLET_ACTIVITY_REQUEST_CODE)
        }

        fun onActivityComplete() {
            synchronized(this) {
                callback?.let { it() }
                callback = null
            }
        }
    }

    private fun doGetCapabilities(client: MobileWalletAdapterClient): MobileWalletAdapterClient.GetCapabilitiesResult? {
        var capabilities: MobileWalletAdapterClient.GetCapabilitiesResult? = null

        try {
            val result = client.getCapabilities().get()
            Log.d(TAG, "Capabilities: $result")
            capabilities = result
        } catch (e: ExecutionException) {
            when (val cause = e.cause) {
                is IOException -> Log.e(TAG, "IO error while sending get_capabilities", cause)
                is TimeoutException ->
                    Log.e(TAG, "Timed out while waiting for get_capabilities result", cause)
                is JsonRpc20Client.JsonRpc20RemoteException ->
                    Log.e(TAG, "Remote exception for get_capabilities", cause)
                is JsonRpc20Client.JsonRpc20Exception ->
                    Log.e(TAG, "JSON-RPC client exception for get_capabilities", cause)
                else -> throw e
            }
        } catch (e: CancellationException) {
            Log.e(TAG, "get_capabilities request was cancelled", e)
        } catch (e: InterruptedException) {
            Log.e(TAG, "get_capabilities request was interrupted", e)
        }

        return capabilities
    }

    private suspend fun <T> localAssociateAndExecute(
        sender: StartActivityForResultSender,
        uriPrefix: Uri? = null,
        action: suspend (MobileWalletAdapterClient) -> T?
    ): T? = coroutineScope {
        return@coroutineScope mobileWalletAdapterClientSem.withPermit {
            val localAssociation = LocalAssociationScenario(Scenario.DEFAULT_CLIENT_TIMEOUT_MS)

            val associationIntent = LocalAssociationIntentCreator.createAssociationIntent(
                uriPrefix,
                localAssociation.port,
                localAssociation.session
            )
            try {
                sender.startActivityForResult(associationIntent) {
                    viewModelScope.launch {
                        // Ensure this coroutine will wrap up in a timely fashion when the launched
                        // activity completes
                        delay(LOCAL_ASSOCIATION_CANCEL_AFTER_WALLET_CLOSED_TIMEOUT_MS)
                        this@coroutineScope.cancel()
                    }
                }
            } catch (e: ActivityNotFoundException) {
                Log.e(TAG, "Failed to start intent=$associationIntent", e)
                // showMessage(R.string.msg_no_wallet_found)
                return@withPermit null
            }

            return@withPermit withContext(Dispatchers.IO) {
                try {
                    val mobileWalletAdapterClient = try {
                        runInterruptible {
                            localAssociation.start().get(LOCAL_ASSOCIATION_START_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                        }
                    } catch (e: InterruptedException) {
                        Log.w(TAG, "Interrupted while waiting for local association to be ready")
                        return@withContext null
                    } catch (e: TimeoutException) {
                        Log.e(TAG, "Timed out waiting for local association to be ready")
                        return@withContext null
                    } catch (e: ExecutionException) {
                        Log.e(TAG, "Failed establishing local association with wallet", e.cause)
                        return@withContext null
                    } catch (e: CancellationException) {
                        Log.e(TAG, "Local association was cancelled before connected", e)
                        return@withContext null
                    }

                    // NOTE: this is a blocking method call, appropriate in the Dispatchers.IO context
                    action(mobileWalletAdapterClient)
                } finally {
                    @Suppress("BlockingMethodInNonBlockingContext") // running in Dispatchers.IO; blocking is appropriate
                    localAssociation.close().get(LOCAL_ASSOCIATION_CLOSE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                }
            }
        }
    }

    interface StartActivityForResultSender {
        fun startActivityForResult(intent: Intent, onActivityCompleteCallback: () -> Unit) // throws ActivityNotFoundException
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