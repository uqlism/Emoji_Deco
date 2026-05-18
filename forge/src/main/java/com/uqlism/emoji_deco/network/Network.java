package com.uqlism.emoji_deco.network;

import com.uqlism.emoji_deco.EmojiDeco;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public class Network {
    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(EmojiDeco.MODID, "main"))
            .networkProtocolVersion(1)
            .simpleChannel();

    public static void register() {
        CHANNEL.messageBuilder(GraffitiUpdatePacket.class)
                .encoder(GraffitiUpdatePacket::encode)
                .decoder(GraffitiUpdatePacket::decode)
                .consumerMainThread(GraffitiUpdatePacket::handle)
                .add();
    }
}
