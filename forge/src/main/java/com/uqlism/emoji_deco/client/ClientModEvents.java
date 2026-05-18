package com.uqlism.emoji_deco.client;

import com.uqlism.emoji_deco.EmojiDeco;
import com.uqlism.emoji_deco.Registration;
import com.uqlism.emoji_deco.client.renderer.GraffitiRenderer;
import com.uqlism.emoji_deco.render.image.ImageGlyphPool;
import com.uqlism.emoji_deco.render.image.source.UrlAllowlist;
import com.uqlism.emoji_deco.text.registry.DecoratorManager;
import com.uqlism.emoji_deco.text.registry.ShortcodeManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
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

import java.util.Optional;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mod.EventBusSubscriber(modid = EmojiDeco.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(ShortcodeManager.INSTANCE);
        event.registerReloadListener(DecoratorManager.INSTANCE);
        event.registerReloadListener(UrlAllowlist.INSTANCE);
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
                new PackLocationInfo("builtin/emoji_deco_starter",
                        Component.translatable("pack.emoji_deco.starter"),
                        PackSource.BUILT_IN, Optional.empty()),
                new Pack.ResourcesSupplier() {
                    @Override public PackResources openPrimary(PackLocationInfo i) { return new PathPackResources(i, packPath); }
                    @Override public PackResources openFull(PackLocationInfo i, Pack.Metadata m) { return new PathPackResources(i, packPath); }
                },
                PackType.CLIENT_RESOURCES,
                new PackSelectionConfig(false, Pack.Position.TOP, false)
        );
        if (pack != null) {
            event.addRepositorySource(packConsumer -> packConsumer.accept(pack));
        }
    }
}
