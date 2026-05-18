package com.uqlism.emoji_deco.platform;

import com.uqlism.emoji_deco.network.GraffitiUpdatePacket;
import net.neoforged.neoforge.network.PacketDistributor;

public class NeoForgeNetworkBridge implements NetworkBridge {

    @Override
    public void sendGraffitiUpdate(GraffitiUpdatePacket packet) {
        PacketDistributor.sendToServer(packet);
    }
}
