package com.nolson.plugins.solanawalletadaptor

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.annotation.GuardedBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.viewModelFactory
import com.getcapacitor.*
import com.getcapacitor.annotation.CapacitorPlugin
import org.json.JSONArray

@CapacitorPlugin(name = "SolanaWalletAdaptor")
class SolanaWalletAdaptorPlugin : Plugin() {

    private val viewModel: SolanaWalletAdaptor by viewModels()
    private val application = Application();
    private val implementation = SolanaWalletAdaptor(application)

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
        val ret = JSObject()
        ret.put(
            "capabilities",
            implementation.getCapabilities()
        )
        call.resolve(ret)

    }

    /*
    @PluginMethod
    fun getCapabilities(call: PluginCall) {
        val ret = JSObject()
        ret.put(
            "capabilities",
            implementation.getCapabilities()
        )
        call.resolve(ret)
    } */


}