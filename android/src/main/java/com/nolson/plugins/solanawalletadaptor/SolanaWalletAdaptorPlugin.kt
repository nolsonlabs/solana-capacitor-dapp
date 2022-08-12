package com.nolson.plugins.solanawalletadaptor;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "SolanaWalletAdaptor")
public class SolanaWalletAdaptorPlugin extends Plugin {

    private SolanaWalletAdaptor implementation = new SolanaWalletAdaptor();

    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", implementation.echo(value));
        call.resolve(ret);
    }
}
