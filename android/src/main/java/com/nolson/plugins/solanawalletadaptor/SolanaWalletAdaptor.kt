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

import com.solana.mobilewalletadapter.clientlib.protocol.JsonRpc20Client
import com.solana.mobilewalletadapter.clientlib.protocol.MobileWalletAdapterClient
import com.solana.mobilewalletadapter.clientlib.scenario.LocalAssociationIntentCreator
import com.solana.mobilewalletadapter.clientlib.scenario.LocalAssociationScenario
import com.solana.mobilewalletadapter.clientlib.scenario.Scenario
import com.solana.mobilewalletadapter.common.ProtocolContract
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
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

data class AppCheckResult(
    val walletName: String,
    val walletHasDeepLinkCapability: Boolean,
    val walletIcon: String,
    val walletInstalled: Boolean
)

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

    fun installedApps(packageManager: PackageManager) : JSObject {
        //Add as many package as we want ot check below comma seprated all in small case
        val checkPackages = listOf(
            "app.phantom",
            "com.y8.slope",
            "com.solflare.mobile",
            "com.solana.mobilewalletadapter.fakewallet"
        )


        var walletArr = JSArray()
        var resultsObj = JSObject()

        val list = packageManager.getInstalledPackages(0)
        val installedPackageIdsList: MutableList<String> = mutableListOf()

        // List of all packages on device
        for (i in list.indices) {
            installedPackageIdsList.add(list[i].packageName)
        }

        for (j in checkPackages.indices) {
            Log.d(TAG, "INDEX")
            Log.d(TAG, j.toString())
            Log.d(TAG, checkPackages[j])

                if(installedPackageIdsList.contains(checkPackages[j])) {
                    val walletName = walletData[checkPackages[j]]?.get("walletName").toString()
                    val walletHasDeepLinkCapability = walletData[checkPackages[j]]?.get("walletHasDeepLinkCapability")
                    val walletIcon = walletData[checkPackages[j]]?.get("walletIcon").toString()
                    val walletInstalled = true

                    var walletObj = JSObject()

                    walletObj.put(
                        "walletName", walletName
                    )
                    walletObj.put(
                        "walletHasDeepLinkCapability", walletHasDeepLinkCapability.toBoolean()
                    )

                    walletObj.put(
                        "walletIcon", walletIcon
                    )
                    walletObj.put(
                        "walletInstalled", walletInstalled
                    )

                    walletArr.put(walletObj)
                    Log.d(TAG, walletObj.toString())

                } else {

                    var walletObj = JSObject()

                    val walletName = walletData[checkPackages[j]]?.get("walletName").toString()
                    val walletHasDeepLinkCapability = walletData[checkPackages[j]]?.get("walletHasDeepLinkCapability")
                    val walletIcon = walletData[checkPackages[j]]?.get("walletIcon").toString()
                    val walletInstalled = false

                    walletObj.put(
                        "walletName", walletName
                    )
                    walletObj.put(
                        "walletHasDeepLinkCapability", walletHasDeepLinkCapability.toBoolean()
                    )

                    walletObj.put(
                        "walletIcon", walletIcon
                    )
                    walletObj.put(
                        "walletInstalled", walletInstalled
                    )
                    walletArr.put(walletObj)
                    Log.d(TAG, walletObj.toString())
                }

        }

        resultsObj.put(
            "dAppPlatform",
            "android"
        )
        resultsObj.put(
            "dAppOs",
            "android"
        )
        resultsObj.put(
            "walletInfo",
            walletArr
        )

        Log.d(TAG, "RESULTS")
        Log.d(TAG, resultsObj.toString())

        return resultsObj
    }



    // Move to another class

    private val phantom = mapOf(
        "walletName" to "Phantom",
        "walletInstalled" to "false",
        "walletHasDeepLinkCapability" to "true",
        "walletIcon" to "data:image/svg+xml;base64,PHN2ZyBmaWxsPSJub25lIiBoZWlnaHQ9IjM0IiB3aWR0aD0iMzQiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGxpbmVhckdyYWRpZW50IGlkPSJhIiB4MT0iLjUiIHgyPSIuNSIgeTE9IjAiIHkyPSIxIj48c3RvcCBvZmZzZXQ9IjAiIHN0b3AtY29sb3I9IiM1MzRiYjEiLz48c3RvcCBvZmZzZXQ9IjEiIHN0b3AtY29sb3I9IiM1NTFiZjkiLz48L2xpbmVhckdyYWRpZW50PjxsaW5lYXJHcmFkaWVudCBpZD0iYiIgeDE9Ii41IiB4Mj0iLjUiIHkxPSIwIiB5Mj0iMSI+PHN0b3Agb2Zmc2V0PSIwIiBzdG9wLWNvbG9yPSIjZmZmIi8+PHN0b3Agb2Zmc2V0PSIxIiBzdG9wLWNvbG9yPSIjZmZmIiBzdG9wLW9wYWNpdHk9Ii44MiIvPjwvbGluZWFyR3JhZGllbnQ+PGNpcmNsZSBjeD0iMTciIGN5PSIxNyIgZmlsbD0idXJsKCNhKSIgcj0iMTciLz48cGF0aCBkPSJtMjkuMTcwMiAxNy4yMDcxaC0yLjk5NjljMC02LjEwNzQtNC45NjgzLTExLjA1ODE3LTExLjA5NzUtMTEuMDU4MTctNi4wNTMyNSAwLTEwLjk3NDYzIDQuODI5NTctMTEuMDk1MDggMTAuODMyMzctLjEyNDYxIDYuMjA1IDUuNzE3NTIgMTEuNTkzMiAxMS45NDUzOCAxMS41OTMyaC43ODM0YzUuNDkwNiAwIDEyLjg0OTctNC4yODI5IDEzLjk5OTUtOS41MDEzLjIxMjMtLjk2MTktLjU1MDItMS44NjYxLTEuNTM4OC0xLjg2NjF6bS0xOC41NDc5LjI3MjFjMCAuODE2Ny0uNjcwMzggMS40ODQ3LTEuNDkwMDEgMS40ODQ3LS44MTk2NCAwLTEuNDg5OTgtLjY2ODMtMS40ODk5OC0xLjQ4NDd2LTIuNDAxOWMwLS44MTY3LjY3MDM0LTEuNDg0NyAxLjQ4OTk4LTEuNDg0Ny44MTk2MyAwIDEuNDkwMDEuNjY4IDEuNDkwMDEgMS40ODQ3em01LjE3MzggMGMwIC44MTY3LS42NzAzIDEuNDg0Ny0xLjQ4OTkgMS40ODQ3LS44MTk3IDAtMS40OS0uNjY4My0xLjQ5LTEuNDg0N3YtMi40MDE5YzAtLjgxNjcuNjcwNi0xLjQ4NDcgMS40OS0xLjQ4NDcuODE5NiAwIDEuNDg5OS42NjggMS40ODk5IDEuNDg0N3oiIGZpbGw9InVybCgjYikiLz48L3N2Zz4K"
    )

    private val solflare = mapOf(
        "walletName" to "Solflare",
        "walletInstalled" to "false",
        "walletHasDeepLinkCapability" to "false",
        "walletIcon" to "data:image/svg+xml;base64,PHN2ZyBmaWxsPSJub25lIiBoZWlnaHQ9IjUwIiB2aWV3Qm94PSIwIDAgNTAgNTAiIHdpZHRoPSI1MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB4bWxuczp4bGluaz0iaHR0cDovL3d3dy53My5vcmcvMTk5OS94bGluayI+PGxpbmVhckdyYWRpZW50IGlkPSJhIj48c3RvcCBvZmZzZXQ9IjAiIHN0b3AtY29sb3I9IiNmZmMxMGIiLz48c3RvcCBvZmZzZXQ9IjEiIHN0b3AtY29sb3I9IiNmYjNmMmUiLz48L2xpbmVhckdyYWRpZW50PjxsaW5lYXJHcmFkaWVudCBpZD0iYiIgZ3JhZGllbnRVbml0cz0idXNlclNwYWNlT25Vc2UiIHgxPSI2LjQ3ODM1IiB4Mj0iMzQuOTEwNyIgeGxpbms6aHJlZj0iI2EiIHkxPSI3LjkyIiB5Mj0iMzMuNjU5MyIvPjxyYWRpYWxHcmFkaWVudCBpZD0iYyIgY3g9IjAiIGN5PSIwIiBncmFkaWVudFRyYW5zZm9ybT0ibWF0cml4KDQuOTkyMTg4MzIgMTIuMDYzODc5NjMgLTEyLjE4MTEzNjU1IDUuMDQwNzEwNzQgMjIuNTIwMiAyMC42MTgzKSIgZ3JhZGllbnRVbml0cz0idXNlclNwYWNlT25Vc2UiIHI9IjEiIHhsaW5rOmhyZWY9IiNhIi8+PHBhdGggZD0ibTI1LjE3MDggNDcuOTEwNGMuNTI1IDAgLjk1MDcuNDIxLjk1MDcuOTQwM3MtLjQyNTcuOTQwMi0uOTUwNy45NDAyLS45NTA3LS40MjA5LS45NTA3LS45NDAyLjQyNTctLjk0MDMuOTUwNy0uOTQwM3ptLTEuMDMyOC00NC45MTU2NWMuNDY0Ni4wMzgzNi44Mzk4LjM5MDQuOTAyNy44NDY4MWwxLjEzMDcgOC4yMTU3NGMuMzc5OCAyLjcxNDMgMy42NTM1IDMuODkwNCA1LjY3NDMgMi4wNDU5bDExLjMyOTEtMTAuMzExNThjLjI3MzMtLjI0ODczLjY5ODktLjIzMTQ5Ljk1MDcuMDM4NTEuMjMwOS4yNDc3Mi4yMzc5LjYyNjk3LjAxNjEuODgyNzdsLTkuODc5MSAxMS4zOTU4Yy0xLjgxODcgMi4wOTQyLS40NzY4IDUuMzY0MyAyLjI5NTYgNS41OTc4bDguNzE2OC44NDAzYy40MzQxLjA0MTguNzUxNy40MjM0LjcwOTMuODUyNC0uMDM0OS4zNTM3LS4zMDc0LjYzOTUtLjY2MjguNjk0OWwtOS4xNTk0IDEuNDMwMmMtMi42NTkzLjM2MjUtMy44NjM2IDMuNTExNy0yLjEzMzkgNS41NTc2bDMuMjIgMy43OTYxYy4yNTk0LjMwNTguMjE4OC43NjE1LS4wOTA4IDEuMDE3OC0uMjYyMi4yMTcyLS42NDE5LjIyNTYtLjkxMzguMDIwM2wtMy45Njk0LTIuOTk3OGMtMi4xNDIxLTEuNjEwOS01LjIyOTctLjI0MTctNS40NTYxIDIuNDI0M2wtLjg3NDcgMTAuMzk3NmMtLjAzNjIuNDI5NS0uNDE3OC43NDg3LS44NTI1LjcxMy0uMzY5LS4wMzAzLS42NjcxLS4zMDk3LS43MTcxLS42NzIxbC0xLjM4NzEtMTAuMDQzN2MtLjM3MTctMi43MTQ0LTMuNjQ1NC0zLjg5MDQtNS42NzQzLTIuMDQ1OWwtMTIuMDUxOTUgMTAuOTc0Yy0uMjQ5NDcuMjI3MS0uNjM4MDkuMjExNC0uODY4LS4wMzUtLjIxMDk0LS4yMjYyLS4yMTczNS0uNTcyNC0uMDE0OTMtLjgwNmwxMC41MTgxOC0xMi4xMzg1YzEuODE4Ny0yLjA5NDIuNDg0OS01LjM2NDQtMi4yODc2LTUuNTk3OGwtOC43MTg3Mi0uODQwNWMtLjQzNDEzLS4wNDE4LS43NTE3Mi0uNDIzNS0uNzA5MzYtLjg1MjQuMDM0OTMtLjM1MzcuMzA3MzktLjYzOTQuNjYyNy0uNjk1bDkuMTUzMzgtMS40Mjk5YzIuNjU5NC0uMzYyNSAzLjg3MTgtMy41MTE3IDIuMTQyMS01LjU1NzZsLTIuMTkyLTIuNTg0MWMtLjMyMTctLjM3OTItLjI3MTMtLjk0NDMuMTEyNi0xLjI2MjEuMzI1My0uMjY5NC43OTYzLS4yNzk3IDEuMTMzNC0uMDI0OWwyLjY5MTggMi4wMzQ3YzIuMTQyMSAxLjYxMDkgNS4yMjk3LjI0MTcgNS40NTYxLTIuNDI0M2wuNzI0MS04LjU1OTk4Yy4wNDU3LS41NDA4LjUyNjUtLjk0MjU3IDEuMDczOS0uODk3Mzd6bS0yMy4xODczMyAyMC40Mzk2NWMuNTI1MDQgMCAuOTUwNjcuNDIxLjk1MDY3Ljk0MDNzLS40MjU2My45NDAzLS45NTA2Ny45NDAzYy0uNTI1MDQxIDAtLjk1MDY3LS40MjEtLjk1MDY3LS45NDAzcy40MjU2MjktLjk0MDMuOTUwNjctLjk0MDN6bTQ3LjY3OTczLS45NTQ3Yy41MjUgMCAuOTUwNy40MjEuOTUwNy45NDAzcy0uNDI1Ny45NDAyLS45NTA3Ljk0MDItLjk1MDctLjQyMDktLjk1MDctLjk0MDIuNDI1Ny0uOTQwMy45NTA3LS45NDAzem0tMjQuNjI5Ni0yMi40Nzk3Yy41MjUgMCAuOTUwNi40MjA5NzMuOTUwNi45NDAyNyAwIC41MTkzLS40MjU2Ljk0MDI3LS45NTA2Ljk0MDI3LS41MjUxIDAtLjk1MDctLjQyMDk3LS45NTA3LS45NDAyNyAwLS41MTkyOTcuNDI1Ni0uOTQwMjcuOTUwNy0uOTQwMjd6IiBmaWxsPSJ1cmwoI2IpIi8+PHBhdGggZD0ibTI0LjU3MSAzMi43NzkyYzQuOTU5NiAwIDguOTgwMi0zLjk3NjUgOC45ODAyLTguODgxOSAwLTQuOTA1My00LjAyMDYtOC44ODE5LTguOTgwMi04Ljg4MTlzLTguOTgwMiAzLjk3NjYtOC45ODAyIDguODgxOWMwIDQuOTA1NCA0LjAyMDYgOC44ODE5IDguOTgwMiA4Ljg4MTl6IiBmaWxsPSJ1cmwoI2MpIi8+PC9zdmc+"
    )

    private val slope = mapOf(
        "walletName" to "Slope",
        "walletInstalled" to "false",
        "walletHasDeepLinkCapability" to "false",
        "walletIcon" to "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgdmlld0JveD0iMCAwIDEyOCAxMjgiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiByeD0iNjQiIGZpbGw9IiM2RTY2RkEiLz4KPHBhdGggZD0iTTI3Ljk0NzUgNTIuMTU5Nkw1MS45ODI2IDI4LjA1NzJMNzIuNjA5OCA3LjY1Mzg5QzczLjg3MzQgNi40MDQwMSA3Ni4wMTc4IDcuMjk5MSA3Ni4wMTc4IDkuMDc2NDJMNzYuMDE4NyA1Mi4xNTlMNTEuOTgzNiA3Ni4xMjY4TDI3Ljk0NzUgNTIuMTU5NloiIGZpbGw9InVybCgjcGFpbnQwX2xpbmVhcl8zNzk1XzI1NTQzKSIvPgo8cGF0aCBkPSJNMTAwLjA1MyA3NS45OTNMNzYuMDE4IDUxLjk1OEw1MS45ODI5IDc1Ljk5MzFMNTEuOTgyOSAxMTguOTI0QzUxLjk4MjkgMTIwLjcwMyA1NC4xMzEyIDEyMS41OTcgNTUuMzkzNyAxMjAuMzQzTDEwMC4wNTMgNzUuOTkzWiIgZmlsbD0idXJsKCNwYWludDFfbGluZWFyXzM3OTVfMjU1NDMpIi8+CjxwYXRoIGQ9Ik0yNy45NDcgNTIuMTYwMUg0NC42ODM5QzQ4LjcxNDcgNTIuMTYwMSA1MS45ODIyIDU1LjQyNzYgNTEuOTgyMiA1OS40NTgzVjc2LjEyNjlIMzUuMjQ1M0MzMS4yMTQ2IDc2LjEyNjkgMjcuOTQ3IDcyLjg1OTQgMjcuOTQ3IDY4LjgyODdWNTIuMTYwMVoiIGZpbGw9IiNGMUYwRkYiLz4KPHBhdGggZD0iTTc2LjAxNzggNTIuMTYwMUg5Mi43NTQ3Qzk2Ljc4NTUgNTIuMTYwMSAxMDAuMDUzIDU1LjQyNzYgMTAwLjA1MyA1OS40NTgzVjc2LjEyNjlIODMuMzE2MUM3OS4yODU0IDc2LjEyNjkgNzYuMDE3OCA3Mi44NTk0IDc2LjAxNzggNjguODI4N1Y1Mi4xNjAxWiIgZmlsbD0iI0YxRjBGRiIvPgo8ZGVmcz4KPGxpbmVhckdyYWRpZW50IGlkPSJwYWludDBfbGluZWFyXzM3OTVfMjU1NDMiIHgxPSI1MS45ODMxIiB5MT0iNy4wNzE1NSIgeDI9IjUxLjk4MzEiIHkyPSI3Ni4xMjY4IiBncmFkaWVudFVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+CjxzdG9wIHN0b3AtY29sb3I9IiNBOEFERkYiLz4KPHN0b3Agb2Zmc2V0PSIwLjY0ODU1NiIgc3RvcC1jb2xvcj0id2hpdGUiLz4KPC9saW5lYXJHcmFkaWVudD4KPGxpbmVhckdyYWRpZW50IGlkPSJwYWludDFfbGluZWFyXzM3OTVfMjU1NDMiIHgxPSI3Ni4wMTgiIHkxPSI1MS45NTgiIHgyPSI3Ni4wMTgiIHkyPSIxMjAuOTI4IiBncmFkaWVudFVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+CjxzdG9wIG9mZnNldD0iMC4yNjA3ODQiIHN0b3AtY29sb3I9IiNCNkJBRkYiLz4KPHN0b3Agb2Zmc2V0PSIxIiBzdG9wLWNvbG9yPSIjRTRFMkZGIi8+CjwvbGluZWFyR3JhZGllbnQ+CjwvZGVmcz4KPC9zdmc+Cg=="
    )

    private val fakewallet = mapOf(
        "walletName" to "FakeWallet",
        "walletInstalled" to "false",
        "walletHasDeepLinkCapability" to "false",
        "walletIcon" to "data:image/svg+xml;base64,PHN2ZyBmaWxsPSJub25lIiBoZWlnaHQ9IjI4IiB3aWR0aD0iMjgiIHZpZXdCb3g9Ii0zIDAgMjggMjgiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGcgZmlsbD0iI0RDQjhGRiI+PHBhdGggZD0iTTE3LjQgMTcuNEgxNXYyLjRoMi40di0yLjRabTEuMi05LjZoLTIuNHYyLjRoMi40VjcuOFoiLz48cGF0aCBkPSJNMjEuNiAzVjBoLTIuNHYzaC0zLjZWMGgtMi40djNoLTIuNHY2LjZINC41YTIuMSAyLjEgMCAxIDEgMC00LjJoMi43VjNINC41QTQuNSA0LjUgMCAwIDAgMCA3LjVWMjRoMjEuNnYtNi42aC0yLjR2NC4ySDIuNFYxMS41Yy41LjMgMS4yLjQgMS44LjVoNy41QTYuNiA2LjYgMCAwIDAgMjQgOVYzaC0yLjRabTAgNS43YTQuMiA0LjIgMCAxIDEtOC40IDBWNS40aDguNHYzLjNaIi8+PC9nPjwvc3ZnPg=="
    )

    private val walletData = mapOf(
        "app.phantom" to phantom,
        "com.solflare.mobile" to solflare,
        "com.y8.slope" to slope,
        "com.solana.mobilewalletadapter.fakewallet" to fakewallet
    )

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

