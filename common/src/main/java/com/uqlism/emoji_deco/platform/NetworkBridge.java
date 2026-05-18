package com.uqlism.emoji_deco.platform;

import com.uqlism.emoji_deco.network.GraffitiUpdatePacket;

/**
 * Platform-agnostic bridge for sending network packets.
 * Implemented by each loader subproject (forge, neoforge) and registered via
 * {@link Services#NETWORK}.
 */
public interface NetworkBridge {

    /** Send a {@link GraffitiUpdatePacket} from the client to the server. */
    void sendGraffitiUpdate(GraffitiUpdatePacket packet);
}
