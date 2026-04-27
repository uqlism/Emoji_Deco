package com.uqlism.emoji_deco.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.uqlism.emoji_deco.Config;
import com.uqlism.emoji_deco.EmojiDeco;
import com.uqlism.emoji_deco.block.GraffitiBlock;
import com.uqlism.emoji_deco.item.GraffitiInkItem;
import com.uqlism.emoji_deco.text.ComponentTransformer;
import com.uqlism.emoji_deco.text.RichTextParser;
import com.uqlism.emoji_deco.client.SuggestionState;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EmojiDeco.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onChatMessage(ClientChatReceivedEvent event) {
        if (!Config.enableChat) return;
        event.setMessage(ComponentTransformer.transform(event.getMessage()));
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!Config.enableItemNames) return;
        List<Component> tooltip = event.getToolTip();
        if (tooltip.isEmpty()) return;
        String raw = tooltip.get(0).getString();
        tooltip.set(0, RichTextParser.parse(raw).toComponent());
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (SuggestionState.suggestions.isEmpty()) return;
        var screen = event.getScreen();
        if (!(screen instanceof ChatScreen)
                && !(screen instanceof AbstractSignEditScreen)
                && !(screen instanceof BookEditScreen)) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth  = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int visible     = Math.min(SuggestionState.suggestions.size(), CompletionRenderer.MAX_VISIBLE);
        int totalHeight = CompletionRenderer.ITEM_HEIGHT * visible;

        int x, baseY;
        if (screen instanceof BookEditScreen) {
            // Book text area: local origin is at (screenWidth-192)/2+36, y=32.
            // Compute the trigger's line number and line-local X from '\n' splits.
            int bookLeft = (screenWidth - 192) / 2 + 36;
            String pageText = SuggestionState.lastInput;
            int tp = Math.min(SuggestionState.triggerPos, pageText.length());
            String beforeTrigger = pageText.substring(0, tp);
            int lastNl = beforeTrigger.lastIndexOf('\n');
            String lineText = lastNl >= 0 ? beforeTrigger.substring(lastNl + 1) : beforeTrigger;
            int lineNum = (int) beforeTrigger.chars().filter(c -> c == '\n').count();
            x = bookLeft + mc.font.width(lineText);
            int cursorLineY = 32 + lineNum * 9;
            // Show below cursor line when there is room; otherwise show above.
            baseY = (cursorLineY + 9 + totalHeight <= screenHeight - 4)
                    ? cursorLineY + 9
                    : cursorLineY - totalHeight;
        } else {
            // Chat and sign: position just above the input bar at the bottom.
            baseY = screenHeight - 14 - totalHeight;
            int triggerPos = Math.min(SuggestionState.triggerPos, SuggestionState.lastInput.length());
            x = 2 + 4 + mc.font.width(SuggestionState.lastInput.substring(0, triggerPos));
        }

        CompletionRenderer.render(
                graphics, mc.font,
                SuggestionState.suggestions, SuggestionState.selectedIndex,
                x, baseY, screenWidth - 2);
    }

    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing event) {
        SuggestionState.clear();
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
        PoseStack poseStack = event.getPoseStack();
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
