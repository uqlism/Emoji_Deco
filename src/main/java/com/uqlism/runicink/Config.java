package com.uqlism.runicink;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = RunicInk.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLE_CHAT = BUILDER
            .comment("Enable rich text formatting in chat messages")
            .define("enableChat", true);

    private static final ForgeConfigSpec.BooleanValue ENABLE_SIGNS = BUILDER
            .comment("Enable rich text formatting on signs")
            .define("enableSigns", true);

    private static final ForgeConfigSpec.BooleanValue ENABLE_ITEM_NAMES = BUILDER
            .comment("Enable rich text formatting in item names (tooltips)")
            .define("enableItemNames", true);

    private static final ForgeConfigSpec.BooleanValue ENABLE_ENTITY_NAMES = BUILDER
            .comment("Enable rich text formatting in entity name tags")
            .define("enableEntityNames", true);

    private static final ForgeConfigSpec.BooleanValue ENABLE_BOOKS = BUILDER
            .comment("Enable rich text formatting in written books")
            .define("enableBooks", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enableChat;
    public static boolean enableSigns;
    public static boolean enableItemNames;
    public static boolean enableEntityNames;
    public static boolean enableBooks;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enableChat = ENABLE_CHAT.get();
        enableSigns = ENABLE_SIGNS.get();
        enableItemNames = ENABLE_ITEM_NAMES.get();
        enableEntityNames = ENABLE_ENTITY_NAMES.get();
        enableBooks = ENABLE_BOOKS.get();
    }
}
