package com.uqlism.emoji_deco;

import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.network.Network;
import com.uqlism.emoji_deco.text.DecoratorManager;
import com.uqlism.emoji_deco.text.ShortcodeManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EmojiDeco.MODID)
public class EmojiDeco {

    public static final String MODID = "emoji_deco";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EmojiDeco(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        Registration.register(modEventBus);
        modEventBus.addListener(this::onCommonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        LOGGER.info("[EmojiDeco] Initialized");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Network::register);
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ShortcodeManager.INSTANCE);
        event.addListener(DecoratorManager.INSTANCE);
    }
}
