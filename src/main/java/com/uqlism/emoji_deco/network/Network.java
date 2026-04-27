package com.uqlism.emoji_deco.network;

import com.uqlism.emoji_deco.EmojiDeco;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class Network {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EmojiDeco.MODID, "main"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, GraffitiUpdatePacket.class,
                GraffitiUpdatePacket::encode,
                GraffitiUpdatePacket::decode,
                GraffitiUpdatePacket::handle);
    }
}
