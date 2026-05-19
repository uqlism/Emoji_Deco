package com.uqlism.emoji_deco.platform;

import com.uqlism.emoji_deco.network.GraffitiUpdatePacket;
import com.uqlism.emoji_deco.network.Network;
import net.minecraftforge.network.PacketDistributor;

public class ForgeNetworkBridge implements NetworkBridge {

    @Override
    public void sendGraffitiUpdate(GraffitiUpdatePacket packet) {
        Network.CHANNEL.send(packet, PacketDistributor.SERVER.noArg());
    }
}
