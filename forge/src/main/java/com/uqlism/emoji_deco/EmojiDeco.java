package com.uqlism.emoji_deco;

import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.network.Network;
import com.uqlism.emoji_deco.platform.ForgeNetworkBridge;
import com.uqlism.emoji_deco.platform.Services;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
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
        Services.register(new ForgeNetworkBridge());
        Registration.register(modEventBus);
        modEventBus.addListener(this::onCommonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        LOGGER.info("[EmojiDeco] Initialized");
        writePidFileIfRequested();
    }

    /** QA 並列実行用: システムプロパティ runicink.qa.pid.file が設定されていれば PID を書き出す。
     *  リリースビルドでも無害（プロパティ未設定なら何もしない）。
     */
    private static void writePidFileIfRequested() {
        String pidFile = System.getProperty("runicink.qa.pid.file");
        if (pidFile != null) {
            try {
                java.nio.file.Files.writeString(java.nio.file.Path.of(pidFile),
                        String.valueOf(ProcessHandle.current().pid()));
                LOGGER.info("[EmojiDeco] Wrote PID {} to {}", ProcessHandle.current().pid(), pidFile);
            } catch (Exception e) {
                LOGGER.warn("[EmojiDeco] Failed to write PID file {}: {}", pidFile, e.getMessage());
            }
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Network::register);
    }
}
