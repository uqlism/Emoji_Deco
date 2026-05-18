package com.uqlism.emoji_deco.platform;

import com.uqlism.emoji_deco.network.GraffitiUpdatePacket;

// TODO(neoforge): Implement packet sending with the NeoForge network API.
// For now this is a no-op stub so that the build passes.
public class NeoForgeNetworkBridge implements NetworkBridge {

    @Override
    public void sendGraffitiUpdate(GraffitiUpdatePacket packet) {
        // TODO: replace with NeoForge equivalent of Network.CHANNEL.send(packet, PacketDistributor.SERVER.noArg())
        throw new UnsupportedOperationException("NeoForge network not yet implemented");
    }
}
