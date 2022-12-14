/*
 * Copyright (c) 2022 Solana Mobile Inc.
 */

package com.nolson.plugins.solanawalletadaptor.common.protocol;

import androidx.annotation.NonNull;

public interface MessageReceiver {
    void receiverConnected(@NonNull MessageSender messageSender);
    void receiverDisconnected();
    void receiverMessageReceived(@NonNull byte[] payload);
}
