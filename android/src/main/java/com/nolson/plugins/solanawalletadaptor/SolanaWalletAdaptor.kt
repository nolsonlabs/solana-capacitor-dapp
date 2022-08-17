package com.nolson.plugins.solanawalletadaptor


import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

import com.getcapacitor.annotation.CapacitorPlugin

import android.app.Application
import android.app.Activity
import androidx.activity.viewModels
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import androidx.annotation.GuardedBy
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.getcapacitor.*

import com.nolson.plugins.solanawalletadaptor.common.ProtocolContract
import com.solana.mobilewalletadapter.clientlib.protocol.JsonRpc20Client
import com.solana.mobilewalletadapter.clientlib.protocol.MobileWalletAdapterClient
import com.solana.mobilewalletadapter.clientlib.scenario.LocalAssociationIntentCreator
import com.solana.mobilewalletadapter.clientlib.scenario.LocalAssociationScenario
import com.solana.mobilewalletadapter.clientlib.scenario.Scenario
import com.solana.mobilewalletadapter.fakedapp.usecase.GetLatestBlockhashUseCase
import com.solana.mobilewalletadapter.fakedapp.usecase.MemoTransactionUseCase
import com.solana.mobilewalletadapter.fakedapp.usecase.RequestAirdropUseCase

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class SolanaWalletAdaptor(application: Application): AndroidViewModel(application) {

    private lateinit var publicKey: ByteArray
    private lateinit var authorizationToken: String
    private val mobileWalletAdapterClientSem = Semaphore(1) // allow only a single MWA connection at a time

    fun echo(value: String): String {
        Log.i("Echo", value)
        return value
    }

    fun isPackageInstalled(packageName: String?, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName!!, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }



    // GET WALLET INFO

    fun checkIsWalletEndpointAvailable(packageManager: PackageManager): Boolean {
        return LocalAssociationIntentCreator.isWalletEndpointAvailable(packageManager)
    }

    fun installedApps(packageManager: PackageManager) :  JSONArray {
        //Add as many package as we want ot check below comma seprated all in small case
        val checkPackges = arrayOf<String>(
            "app.phantom",
            "com.y8.slope",
            "com.solflare.mobile",
            "com.solana.mobilewalletadapter.fakewallet"
        )
        val installedPackages = JSONArray()
        //val list: installedPackages<Int> = arr.toMutableList()

        val list = packageManager.getInstalledPackages(0)
        for (i in list.indices) {
            val packageInfo = list[i]
            if (packageInfo!!.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                val appName = packageInfo.applicationInfo.loadLabel(packageManager).toString()
                val appPackageName = packageInfo.applicationInfo.packageName
                if(checkPackges.contains(appPackageName)) {
                    //here we found our package installed. add it in installed array
                    installedPackages.put(appPackageName)
                }
            }
        }


        Log.d(TAG, installedPackages.toString())

        return installedPackages
    }

    // FAKE WALLET FUNCTIONALITY

    fun authorize(sender: StartActivityForResultSender, call: PluginCall, callOrigin: String) = viewModelScope.launch {
        val result = localAssociateAndExecute(sender) { client ->
            doAuthorize(call, client, callOrigin)
        }

        if (result == true) Log.d(TAG, "AUTHORIZE: msg_request_succeeded") else Log.d(TAG, "AUTHORIZE: msg_request_failed")

    }

    fun returnAuthCall(call: PluginCall, authorized: Boolean, authToken: String, pubKey: String) {
        val ret = JSObject()
        ret.put(
            "authorized",
            authorized
        )
        ret.put(
            "authToken",
            authToken
        )
        ret.put(
            "publicKey",
            pubKey
        )
        call.resolve(ret)
    }

    fun returnReAuthCall(call: PluginCall, authorized: Boolean, authToken: String, pubKey: String) {
        val ret = JSObject()
        ret.put(
            "reauthorized",
            authorized
        )
        ret.put(
            "authToken",
            authToken
        )
        ret.put(
            "publicKey",
            pubKey
        )
        call.resolve(ret)
    }

    fun doAuthorize(call: PluginCall, client: MobileWalletAdapterClient, callOrigin: String): Boolean {
        var authorized = false
        try {
            val result = client.authorize(
                Uri.parse("https://solana.com"),
                Uri.parse("favicon.ico"),
                "Solana",
                ProtocolContract.CLUSTER_TESTNET
            ).get()
            publicKey = result.publicKey
            authorizationToken = result.authToken
            if (callOrigin == "plugin") {
                returnAuthCall(call, true, result.authToken, result.publicKey.toString())
            }
            Log.d(TAG, "Authorized: $result")
            return true

        } catch (e: ExecutionException) {
            when (val cause = e.cause) {
                is IOException ->
                     Log.e(TAG, "IO error while sending authorize", cause)
                is TimeoutException ->
                    Log.e(TAG, "Timed out while waiting for authorize result", cause)
                is JsonRpc20Client.JsonRpc20RemoteException ->
                    when (cause.code) {
                        ProtocolContract.ERROR_AUTHORIZATION_FAILED ->
                          Log.e(TAG, "Not authorized", cause)
                        else ->
                            Log.e(TAG, "Remote exception for authorize", cause)
                    }
                is MobileWalletAdapterClient.InsecureWalletEndpointUriException ->

                     Log.e(TAG, "authorize result contained a non-HTTPS wallet base URI", e)
                is JsonRpc20Client.JsonRpc20Exception ->

                     Log.e(TAG, "JSON-RPC client exception for authorize", cause)
                else -> throw e
            }
        } catch (e: CancellationException) {

            Log.e(TAG, "authorize request was cancelled", e)
        } catch (e: InterruptedException) {

             Log.e(TAG, "authorize request was interrupted", e)
        }

        return authorized
    }

    fun reauthorize(call: PluginCall, sender: StartActivityForResultSender, authToken: String, callOrigin: String) = viewModelScope.launch {
        val result = localAssociateAndExecute(sender) { client ->
            doReauthorize(call, client, authToken, callOrigin)
        }

        if (result == true) Log.d( TAG,"Reauth msg_request_succeeded") else Log.d( TAG,"Reauth msg_request_succeeded")
    }

    private fun doReauthorize(call: PluginCall, client: MobileWalletAdapterClient, authToken: String, callOrigin: String): Boolean {
        Log.d(TAG, "authToken")
        Log.d(TAG, authToken)
        var reauthorized = false
        try {
            val result = client.reauthorize(
                Uri.parse("https://solana.com"),
                Uri.parse("favicon.ico"),
                "Solana",
                authToken
            ).get()

            publicKey = result.publicKey
            authorizationToken = result.authToken
            if (callOrigin == "plugin") {
                returnReAuthCall(call, true, result.authToken, result.publicKey.toString())
            }
            Log.d(TAG, "Reauthorized: $result")

            reauthorized = true
        } catch (e: ExecutionException) {
            if (callOrigin == "plugin") {
                returnReAuthCall(call, false, "", "")
            }
            when (val cause = e.cause) {
                is IOException -> Log.e(TAG, "IO error while sending reauthorize", cause)
                is TimeoutException ->
                    Log.e(TAG, "Timed out while waiting for reauthorize result", cause)
                is JsonRpc20Client.JsonRpc20RemoteException ->
                    when (cause.code) {
                        com.solana.mobilewalletadapter.common.ProtocolContract.ERROR_AUTHORIZATION_FAILED -> {
                            Log.e(TAG, "Not reauthorized", cause)
                        }
                        else ->
                            Log.e(TAG, "Remote exception for reauthorize", cause)
                    }
                is MobileWalletAdapterClient.InsecureWalletEndpointUriException ->
                    Log.e(TAG, "reauthorize result contained a non-HTTPS wallet base URI", e)
                is JsonRpc20Client.JsonRpc20Exception ->
                    Log.e(TAG, "JSON-RPC client exception for reauthorize", cause)
                else -> throw e
            }
        } catch (e: CancellationException) {
            Log.e(TAG, "reauthorize request was cancelled", e)
        } catch (e: InterruptedException) {
            Log.e(TAG, "reauthorize request was interrupted", e)
        }

        return reauthorized
    }

    fun deauthorize(call: PluginCall, sender: StartActivityForResultSender, authToken: String) = viewModelScope.launch {
        val result = localAssociateAndExecute(sender) { client ->
            doDeauthorize(call, client, authToken)
        }

        if (result == true) "msg_request_succeeded" else "msg_request_failed"
    }

    private fun doDeauthorize(call: PluginCall, client: MobileWalletAdapterClient, authToken: String): Boolean {
        var deauthorized = false
        try {
            client.deauthorize(authToken).get()
            Log.d(TAG, "Deauthorized")
            returnDeAuthCall(call, true)
            deauthorized = true
        } catch (e: ExecutionException) {
            when (val cause = e.cause) {
                is IOException -> {
                    returnDeAuthCall(call, false)
                    Log.e(TAG, "IO error while sending deauthorize", cause)
                }
                is TimeoutException -> {
                    returnDeAuthCall(call, false)
                    Log.e(TAG, "Timed out while waiting for deauthorize result", cause)
                }

                is JsonRpc20Client.JsonRpc20RemoteException -> {
                    returnDeAuthCall(call, false)
                    Log.e(TAG, "Remote exception for deauthorize", cause)
                }

                is JsonRpc20Client.JsonRpc20Exception -> {

                    returnDeAuthCall(call, false)
                    Log.e(TAG, "JSON-RPC client exception for deauthorize", cause)
                }

                else -> {
                    returnDeAuthCall(call, false)
                    throw e
                }
            }
        } catch (e: CancellationException) {
            returnDeAuthCall(call, false)
            Log.e(TAG, "deauthorize request was cancelled", e)
        } catch (e: InterruptedException) {
            returnDeAuthCall(call, false)
            Log.e(TAG, "deauthorize request was interrupted", e)
        }

        return deauthorized
    }

    fun returnDeAuthCall(call: PluginCall, authorized: Boolean) {
        val ret = JSObject()
        ret.put(
            "deauthorized",
            authorized
        )
        call.resolve(ret)
    }

    // GET CAPABILITIES FUNCTION

    fun getCapabilities(sender: StartActivityForResultSender) = viewModelScope.launch {
            val result = localAssociateAndExecute(sender) { client ->
                doGetCapabilities(client)
            }

            if (result != null) Log.d(TAG, "msg_request_succeeded") else Log.d(TAG, "msg_request_failed")
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
        Log.d(TAG, "CAPABILITIES OBJECT")
        Log.d(TAG, capabilities.toString())
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

    // REQUEST AIRDROP
    fun requestAirdrop(call: PluginCall, sender: StartActivityForResultSender, authToken: String) = viewModelScope.launch {
        try {
            localAssociateAndExecute(sender) { client ->
                doReauthorize(call, client, authToken, "")
                RequestAirdropUseCase(TESTNET_RPC_URI, publicKey)
                returnSignCall(call, true)
                Log.d(TAG, "Airdrop request sent")
            }
        } catch (e: RequestAirdropUseCase.AirdropFailedException) {
            returnSignCall(call, false)
            Log.e(TAG, "Airdrop request failed", e)
        }
    }

    // TRANSACTION, MESSAGES & SIGNING
    // SIGN TRANSACTIONS

    fun signTransactions(call: PluginCall, sender: StartActivityForResultSender, numTransactions: Int, authToken: String, pubKey: ByteArray) = viewModelScope.launch {
        val latestBlockhash = viewModelScope.async(Dispatchers.IO) {
            GetLatestBlockhashUseCase(TESTNET_RPC_URI)
        }

        val signedTransactions = localAssociateAndExecute(sender) { client ->
            val authorized = doReauthorize(call, client, authToken, "")
            if (!authorized) {
                return@localAssociateAndExecute null
            }
            val (blockhash, _) = try {
                latestBlockhash.await()
            } catch (e: GetLatestBlockhashUseCase.GetLatestBlockhashFailedException) {
                Log.e(TAG, "Failed retrieving latest blockhash", e)
                return@localAssociateAndExecute null
            }
            val transactions = Array(numTransactions) {
                MemoTransactionUseCase.create(publicKey, blockhash)
            }
            doSignTransactions(client, transactions)
        }

        if (signedTransactions != null) {
            val verified = signedTransactions.map { txn ->
                try {
                    MemoTransactionUseCase.verify(pubKey, txn)
                    true
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Memo transaction signature verification failed", e)
                    false
                }
            }

            if (verified.all { it }) {
                returnSignCall(call, true)
            } else {
                returnSignCall(call, false)
            }
        } else {
            returnSignCall(call, false)
            Log.w(TAG, "No signed transactions returned; skipping verification")
        }
    }

    fun returnSignCall(call: PluginCall, success: Boolean) {
        Log.d(TAG, "RETURN SIGN CALLED")
        val ret = JSObject()
        ret.put(
            "success",
            success
        )
        call.resolve(ret)
    }

    fun returnAuthAndSignTransactionsCall(call: PluginCall, success: Boolean) {
        val ret = JSObject()
        ret.put(
            "success",
            success
        )
        ret.put(
            "publicKey",
            publicKey.toString()
        )
        ret.put(
            "authToken",
            authorizationToken.toString()
        )
        call.resolve(ret)
    }

    private fun doSignTransactions(
        client: MobileWalletAdapterClient,
        transactions: Array<ByteArray>
    ): Array<ByteArray>? {
        var signedTransactions: Array<ByteArray>? = null
        try {
            val result = client.signTransactions(transactions).get()
            Log.d(TAG, "Signed transaction(s): $result")
            signedTransactions = result.signedPayloads
        } catch (e: ExecutionException) {
            when (val cause = e.cause) {
                is IOException -> Log.e(TAG, "IO error while sending sign_transactions", cause)
                is TimeoutException ->
                    Log.e(TAG, "Timed out while waiting for sign_transactions result", cause)
                is MobileWalletAdapterClient.InvalidPayloadsException ->
                    Log.e(TAG, "Transaction payloads invalid", cause)
                is JsonRpc20Client.JsonRpc20RemoteException ->
                    when (cause.code) {
                        com.solana.mobilewalletadapter.common.ProtocolContract.ERROR_AUTHORIZATION_FAILED -> Log.e(TAG, "Authorization invalid, authorization or reauthorization required", cause)
                        com.solana.mobilewalletadapter.common.ProtocolContract.ERROR_NOT_SIGNED -> Log.e(TAG, "User did not authorize signing", cause)
                        com.solana.mobilewalletadapter.common.ProtocolContract.ERROR_TOO_MANY_PAYLOADS -> Log.e(TAG, "Too many payloads to sign", cause)
                        else -> Log.e(TAG, "Remote exception for sign_transactions", cause)
                    }
                is JsonRpc20Client.JsonRpc20Exception ->
                    Log.e(TAG, "JSON-RPC client exception for sign_transactions", cause)
                else -> throw e
            }
        } catch (e: CancellationException) {
            Log.e(TAG, "sign_transactions request was cancelled", e)
        } catch (e: InterruptedException) {
            Log.e(TAG, "sign_transactions request was interrupted", e)
        }

        return signedTransactions
    }

    // Authorize & sign

    fun authorizeAndSignTransactions(call: PluginCall, sender: StartActivityForResultSender) = viewModelScope.launch {
        val latestBlockhash = viewModelScope.async(Dispatchers.IO) {
            GetLatestBlockhashUseCase(TESTNET_RPC_URI)
        }

        val signedTransactions = localAssociateAndExecute(sender) { client ->
            val authorized = doAuthorize(call, client, "")
            if (!authorized) {
                return@localAssociateAndExecute null
            }
            val (blockhash, _) = try {
                latestBlockhash.await()
            } catch (e: GetLatestBlockhashUseCase.GetLatestBlockhashFailedException) {
                Log.e(TAG, "Failed retrieving latest blockhash", e)
                return@localAssociateAndExecute null
            }
            val transactions = Array(1) {
                MemoTransactionUseCase.create(publicKey, blockhash)
            }
            doSignTransactions(client, transactions)
        }

        if (signedTransactions != null) {
            val verified = signedTransactions.map { txn ->
                try {
                    MemoTransactionUseCase.verify(publicKey, txn)
                    true
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Memo transaction signature verification failed", e)
                    false
                }
            }

            if (verified.all { it }) {
                returnAuthAndSignTransactionsCall(call, true)
            } else {
                returnAuthAndSignTransactionsCall(call, false)
            }
        } else {
            returnAuthAndSignTransactionsCall(call, false)
            Log.w(TAG, "No signed transactions returned; skipping verification")
        }
    }

    // Sign messages

    fun signMessages(call: PluginCall, sender: StartActivityForResultSender, numMessages: Int, authToken: String) = viewModelScope.launch {
        val signedMessages = localAssociateAndExecute(sender) { client ->
            val authorized = doReauthorize(call, client, authToken, "")
            if (!authorized) {
                return@localAssociateAndExecute null
            }
            val messages = Array(numMessages) {
                Random.nextBytes(16384)
            }
            doSignMessages(client, messages, arrayOf(publicKey))
        }

        if (signedMessages != null) {
            returnSignCall(call, true)
        } else {
            returnSignCall(call, false)
        }

    }

    private fun doSignMessages(
        client: MobileWalletAdapterClient,
        messages: Array<ByteArray>,
        addresses: Array<ByteArray>
    ): Array<ByteArray>? {
        var signedMessages: Array<ByteArray>? = null
        try {
            val result = client.signMessages(messages, addresses).get()
            Log.d(TAG, "Signed message(s): $result")
            signedMessages = result.signedPayloads
        } catch (e: ExecutionException) {
            when (val cause = e.cause) {
                is IOException -> Log.e(TAG, "IO error while sending sign_messages", cause)
                is TimeoutException ->
                    Log.e(TAG, "Timed out while waiting for sign_messages result", cause)
                is MobileWalletAdapterClient.InvalidPayloadsException ->
                    Log.e(TAG, "Message payloads invalid", cause)
                is JsonRpc20Client.JsonRpc20RemoteException ->
                    when (cause.code) {
                        com.solana.mobilewalletadapter.common.ProtocolContract.ERROR_AUTHORIZATION_FAILED -> Log.e(TAG, "Authorization invalid, authorization or reauthorization required", cause)
                        com.solana.mobilewalletadapter.common.ProtocolContract.ERROR_NOT_SIGNED -> Log.e(TAG, "User did not authorize signing", cause)
                        com.solana.mobilewalletadapter.common.ProtocolContract.ERROR_TOO_MANY_PAYLOADS -> Log.e(TAG, "Too many payloads to sign", cause)
                        else -> Log.e(TAG, "Remote exception for sign_messages", cause)
                    }
                is JsonRpc20Client.JsonRpc20Exception ->
                    Log.e(TAG, "JSON-RPC client exception for sign_messages", cause)
                else -> throw e
            }
        } catch (e: CancellationException) {
            Log.e(TAG, "sign_messages request was cancelled", e)
        } catch (e: InterruptedException) {
            Log.e(TAG, "sign_messages request was interrupted", e)
        }

        return signedMessages
    }

    // Sign and send Transactions

    fun signAndSendTransactions(call: PluginCall, sender: StartActivityForResultSender, numTransactions: Int, authToken: String) = viewModelScope.launch {
        val latestBlockhash = viewModelScope.async(Dispatchers.IO) {
            GetLatestBlockhashUseCase(TESTNET_RPC_URI)
        }

        val signatures = localAssociateAndExecute(sender) { client ->
            val authorized = doReauthorize(call, client, authToken, "")
            if (!authorized) {
                return@localAssociateAndExecute null
            }
            val (blockhash, slot) = try {
                latestBlockhash.await()
            } catch (e: GetLatestBlockhashUseCase.GetLatestBlockhashFailedException) {
                Log.e(TAG, "Failed retrieving latest blockhash", e)
                return@localAssociateAndExecute null
            }
            val transactions = Array(numTransactions) {
                MemoTransactionUseCase.create(publicKey, blockhash)
            }
            doSignAndSendTransactions(call, client, transactions, slot)
        }
    }

    private fun doSignAndSendTransactions(
        call: PluginCall,
        client: MobileWalletAdapterClient,
        transactions: Array<ByteArray>,
        minContextSlot: Int? = null
    ): Array<ByteArray>? {
        var signatures: Array<ByteArray>? = null
        try {
            val result = client.signAndSendTransactions(transactions, minContextSlot).get()
            Log.d(TAG, "Signatures: ${result.signatures.contentToString()}")
            signatures = result.signatures
            returnSignCall(call, true)
        } catch (e: ExecutionException) {
            returnSignCall(call, false)
            when (val cause = e.cause) {
                is IOException ->
                    Log.e(TAG, "IO error while sending sign_and_send_transactions", cause)
                is TimeoutException ->
                    Log.e(TAG, "Timed out while waiting for sign_and_send_transactions result", cause)
                is MobileWalletAdapterClient.InvalidPayloadsException ->
                    Log.e(TAG, "Transaction payloads invalid", cause)
                is MobileWalletAdapterClient.NotSubmittedException -> {
                    Log.e(TAG, "Not all transactions were submitted", cause)
                }
                is JsonRpc20Client.JsonRpc20RemoteException ->
                    when (cause.code) {
                        com.solana.mobilewalletadapter.common.ProtocolContract.ERROR_AUTHORIZATION_FAILED -> Log.e(TAG, "Authorization invalid, authorization or reauthorization required", cause)
                        com.solana.mobilewalletadapter.common.ProtocolContract.ERROR_NOT_SIGNED -> Log.e(TAG, "User did not authorize signing", cause)
                        com.solana.mobilewalletadapter.common.ProtocolContract.ERROR_TOO_MANY_PAYLOADS -> Log.e(TAG, "Too many payloads to sign", cause)
                        else -> Log.e(TAG, "Remote exception for sign_and_send_transactions", cause)
                    }
                is JsonRpc20Client.JsonRpc20Exception ->
                    Log.e(TAG, "JSON-RPC client exception for sign_and_send_transactions", cause)
                else -> throw e
            }
        } catch (e: CancellationException) {
            Log.e(TAG, "sign_and_send_transactions request was cancelled", e)
        } catch (e: InterruptedException) {
            Log.e(TAG, "sign_and_send_transactions request was interrupted", e)
        }

        return signatures
    }

    // Interfaces and companion objects

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

