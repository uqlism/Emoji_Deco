package com.uqlism.emoji_deco.client;

import com.uqlism.emoji_deco.EmojiDeco;
import com.uqlism.emoji_deco.Registration;
import com.uqlism.emoji_deco.client.renderer.GraffitiRenderer;
import com.uqlism.emoji_deco.render.registry.TextureRegistry;
import com.uqlism.emoji_deco.text.DecoratorManager;
import com.uqlism.emoji_deco.text.ShortcodeManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mod.EventBusSubscriber(modid = EmojiDeco.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(ShortcodeManager.INSTANCE);
        event.registerReloadListener(DecoratorManager.INSTANCE);
        event.registerReloadListener(new net.minecraft.server.packs.resources.PreparableReloadListener() {
            @Override
            public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager rm,
                                                   ProfilerFiller prep, ProfilerFiller apply,
                                                   Executor prepExec, Executor applyExec) {
                return CompletableFuture.runAsync(() -> {}, prepExec)
                        .thenCompose(barrier::wait)
                        .thenRunAsync(TextureRegistry::onResourceReload, applyExec);
            }
        });
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.GRAFFITI_BLOCK_ENTITY.get(), GraffitiRenderer::new);
    }
}
