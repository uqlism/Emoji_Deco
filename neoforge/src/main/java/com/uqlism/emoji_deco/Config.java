package com.uqlism.emoji_deco;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

@EventBusSubscriber(modid = EmojiDeco.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ENABLE_CHAT = BUILDER
            .comment("Enable rich text formatting in chat messages")
            .define("enableChat", true);

    private static final ModConfigSpec.BooleanValue ENABLE_SIGNS = BUILDER
            .comment("Enable rich text formatting on signs")
            .define("enableSigns", true);

    private static final ModConfigSpec.BooleanValue ENABLE_ITEM_NAMES = BUILDER
            .comment("Enable rich text formatting in item names (tooltips)")
            .define("enableItemNames", true);

    private static final ModConfigSpec.BooleanValue ENABLE_ENTITY_NAMES = BUILDER
            .comment("Enable rich text formatting in entity name tags")
            .define("enableEntityNames", true);

    private static final ModConfigSpec.BooleanValue ENABLE_BOOKS = BUILDER
            .comment("Enable rich text formatting in written books")
            .define("enableBooks", true);

    private static final ModConfigSpec.BooleanValue ENABLE_GUI = BUILDER
            .comment("Enable rich text formatting in titles, subtitles, and action bar")
            .define("enableGui", true);

    private static final ModConfigSpec.BooleanValue ENABLE_URL_FETCH = BUILDER
            .comment("Allow emoji_deco:fetch_url to make HTTP requests.",
                     "Actual domains are controlled by allowed_domains in each resource pack's url_allowlist.json.",
                     "Set to false to disable all URL fetching regardless of resource pack settings.")
            .define("enableUrlFetch", true);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> ADDITIONAL_ALLOWED_DOMAINS = BUILDER
            .comment("Extra domains the player personally trusts, in addition to resource pack allow lists.",
                     "Supports exact match (\"example.com\") and wildcard subdomain (\"*.example.com\").",
                     "Only takes effect when enableUrlFetch = true.")
            .defineListAllowEmpty("additionalAllowedDomains", List.of(),
                    obj -> obj instanceof String s && !s.isBlank());

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean enableChat;
    public static boolean enableSigns;
    public static boolean enableItemNames;
    public static boolean enableEntityNames;
    public static boolean enableBooks;
    public static boolean enableGui;
    public static boolean enableUrlFetch;
    public static List<String> additionalAllowedDomains;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enableChat = ENABLE_CHAT.get();
        enableSigns = ENABLE_SIGNS.get();
        enableItemNames = ENABLE_ITEM_NAMES.get();
        enableEntityNames = ENABLE_ENTITY_NAMES.get();
        enableBooks = ENABLE_BOOKS.get();
        enableGui = ENABLE_GUI.get();
        enableUrlFetch = ENABLE_URL_FETCH.get();
        additionalAllowedDomains = ADDITIONAL_ALLOWED_DOMAINS.get().stream()
                .map(s -> (String) s)
                .toList();
    }
}
