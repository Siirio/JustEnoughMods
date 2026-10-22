package com.siirio.jemeconomy.client;

import com.siirio.jemeconomy.network.WalletStatusPacket;

/** Legacy snapshot compatibility; standalone wallet entry points have been retired. */
public final class WalletClientState {
    private WalletClientState() {}
    public static void accept(WalletStatusPacket packet) {}
}
