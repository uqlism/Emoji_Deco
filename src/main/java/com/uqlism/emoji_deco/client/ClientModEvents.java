package com.uqlism.emoji_deco.client;

import com.uqlism.emoji_deco.EmojiDeco;
import com.uqlism.emoji_deco.Registration;
import com.uqlism.emoji_deco.client.renderer.GraffitiRenderer;
import com.uqlism.emoji_deco.render.image.ImageGlyphPool;
import com.uqlism.emoji_deco.text.registry.DecoratorManager;
import com.uqlism.emoji_deco.text.registry.ShortcodeManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.resource.PathPackResources;

import java.nio.file.Path;
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
                        .thenRunAsync(ImageGlyphPool::onResourceReload, applyExec);
            }
        });
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.GRAFFITI_BLOCK_ENTITY.get(), GraffitiRenderer::new);
    }

    @SubscribeEvent
    public static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        Path packPath = ModList.get()
                .getModFileById(EmojiDeco.MODID)
                .getFile()
                .findResource("resourcepacks", "emoji_deco_starter");
        Pack pack = Pack.readMetaAndCreate(
                "builtin/emoji_deco_starter",
                Component.translatable("pack.emoji_deco.starter"),
                false,
                id -> new PathPackResources(id, true, packPath),
                PackType.CLIENT_RESOURCES,
                Pack.Position.TOP,
                PackSource.BUILT_IN
        );
        if (pack != null) {
            event.addRepositorySource(packConsumer -> packConsumer.accept(pack));
        }
    }
}
