/*
 * Copyright (c) 2022 Solana Mobile Inc.
 */

package com.nolson.plugins.solanawalletadaptor.common.protocol;

import androidx.annotation.NonNull;

import java.io.IOException;

public interface MessageSender {
    void send(@NonNull byte[] message) throws IOException;
}
