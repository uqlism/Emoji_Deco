package com.uqlism.emoji_deco.platform;

/**
 * Static registry for platform service implementations.
 * Each loader (forge / neoforge) calls {@link #register(NetworkBridge)} during
 * mod initialisation before any client-side screen can be opened.
 */
public final class Services {

    private Services() {}

    private static volatile NetworkBridge network;

    public static void register(NetworkBridge bridge) {
        network = bridge;
    }

    /** Returns the registered {@link NetworkBridge}. Throws if not yet registered. */
    public static NetworkBridge network() {
        NetworkBridge b = network;
        if (b == null) throw new IllegalStateException("NetworkBridge has not been registered yet");
        return b;
    }
}
