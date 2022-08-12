package com.nolson.plugins.solanawalletadaptor

import android.app.Activity
import android.app.Application
import android.content.Context
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.getcapacitor.*
import com.getcapacitor.annotation.CapacitorPlugin
import org.json.JSONArray

@CapacitorPlugin(name = "SolanaWalletAdaptor")
class SolanaWalletAdaptorPlugin : Plugin() {
    private val implementation = SolanaWalletAdaptor()

    @PluginMethod
    fun echo(call: PluginCall) {
        val value = call.getString("value")
        val ret = JSObject()
        // ret.put("value", implementation.echo(value))
        call.resolve(ret)
    }

    @PluginMethod
    fun isPackageInstalled(call: PluginCall) {
        val packageName = call.getString("packageName")
        val ret = JSObject()
        ret.put(
            "installed",
            implementation.isPackageInstalled(packageName, activity.packageManager)
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
        val context = Plugin().context
        context.applicationContext

        val ret = JSObject()
        ret.put(
            "capabilities",
            implementation.getCapabilities(context)
        )
        call.resolve(ret)
    }

    @PluginMethod
    fun listAvailableWallets(call: PluginCall) {
        val ret = JSObject()
        val arr = JSONArray()
        arr.put("phantom")
        arr.put("solflare-viaandroid")
        ret.put("wallets", arr)
        call.resolve(ret)
    }
}