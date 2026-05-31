package com.uqlism.emoji_deco.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.uqlism.emoji_deco.Config;
import com.uqlism.emoji_deco.EmojiDeco;
import com.uqlism.emoji_deco.block.GraffitiBlock;
import com.uqlism.emoji_deco.item.GraffitiInkItem;
import com.uqlism.emoji_deco.render.image.ImageGlyphPool;
import com.uqlism.emoji_deco.render.image.source.SkinSourceResolver;
import com.uqlism.emoji_deco.text.ComponentConverter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid = EmojiDeco.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!Config.enableItemNames) return;
        List<Component> tooltip = event.getToolTip();
        if (tooltip.isEmpty()) return;
        tooltip.set(0, ComponentConverter.toComponent(tooltip.get(0)));
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        var screen = event.getScreen();
        if (!(screen instanceof ChatScreen)
                && !(screen instanceof AbstractSignEditScreen)
                && !(screen instanceof BookEditScreen)) return;

        SuggestionState ss = SuggestionState.of(screen);
        ss.computeIfReady();
        if (ss.suggestions.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth  = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int visible     = Math.min(ss.suggestions.size(), CompletionRenderer.MAX_VISIBLE);
        int totalHeight = CompletionRenderer.ITEM_HEIGHT * visible;

        int x, baseY;
        if (screen instanceof BookEditScreen) {
            int bookLeft = (screenWidth - 192) / 2 + 36;
            String pageText = ss.lastInput;
            int tp = Math.min(ss.triggerPos, pageText.length());
            String beforeTrigger = pageText.substring(0, tp);
            int lastNl = beforeTrigger.lastIndexOf('\n');
            String lineText = lastNl >= 0 ? beforeTrigger.substring(lastNl + 1) : beforeTrigger;
            int lineNum = (int) beforeTrigger.chars().filter(c -> c == '\n').count();
            x = bookLeft + mc.font.width(lineText);
            int cursorLineY = 32 + lineNum * 9;
            baseY = (cursorLineY + 9 + totalHeight <= screenHeight - 4)
                    ? cursorLineY + 9
                    : cursorLineY - totalHeight;
        } else {
            baseY = screenHeight - 14 - totalHeight;
            int triggerPos = Math.min(ss.triggerPos, ss.lastInput.length());
            x = 2 + 4 + mc.font.width(ss.lastInput.substring(0, triggerPos));
        }

        CompletionRenderer.render(
                graphics, mc.font,
                ss.suggestions, ss.selectedIndex,
                x, baseY, screenWidth - 2);
    }

    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing event) {
        SuggestionState.of(event.getScreen()).clear();
    }

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        SkinSourceResolver.clearCache();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ImageGlyphPool.tick();
    }

    private static final int HIGHLIGHT_RANGE = 10;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        boolean holdingInk = mc.player.getMainHandItem().getItem() instanceof GraffitiInkItem
                          || mc.player.getOffhandItem().getItem() instanceof GraffitiInkItem;
        if (!holdingInk) return;

        Vec3 camPos = event.getCamera().getPosition();
        // renderLevel の poseStack は単位行列。GPU の ModelViewMat ユニフォームがカメラ変換を担う。
        // ここで ModelViewMatrix を掛けると二重適用になり座標が壊れる。
        PoseStack poseStack = new PoseStack();
        VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());

        BlockPos center = mc.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-HIGHLIGHT_RANGE, -HIGHLIGHT_RANGE, -HIGHLIGHT_RANGE),
                center.offset(HIGHLIGHT_RANGE, HIGHLIGHT_RANGE, HIGHLIGHT_RANGE))) {
            BlockState state = mc.level.getBlockState(pos);
            if (!(state.getBlock() instanceof GraffitiBlock)) continue;

            VoxelShape shape = state.getShape(mc.level, pos, CollisionContext.empty());
            if (shape.isEmpty()) continue;

            poseStack.pushPose();
            poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
            for (AABB box : shape.toAabbs()) {
                LevelRenderer.renderLineBox(poseStack, consumer, box, 1f, 1f, 0f, 0.8f);
            }
            poseStack.popPose();
        }

        mc.renderBuffers().bufferSource().endBatch(RenderType.lines());
    }
}
