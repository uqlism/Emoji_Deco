package com.uqlism.emoji_deco;

import com.mojang.logging.LogUtils;
import com.uqlism.emoji_deco.platform.NeoForgeNetworkBridge;
import com.uqlism.emoji_deco.platform.Services;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(EmojiDeco.MODID)
public class EmojiDeco {

    public static final String MODID = "emoji_deco";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EmojiDeco(IEventBus modEventBus, ModContainer container) {
        Services.register(new NeoForgeNetworkBridge());
        Registration.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        LOGGER.info("[EmojiDeco] Initialized (NeoForge)");
        writePidFileIfRequested();
    }

    /** QA 並列実行用: システムプロパティ runicink.qa.pid.file が設定されていれば PID を書き出す。 */
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
}
