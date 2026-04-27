package com.uqlism.emoji_deco.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.uqlism.emoji_deco.block.GraffitiAlignment;
import com.uqlism.emoji_deco.block.GraffitiBlock;
import com.uqlism.emoji_deco.block.GraffitiBlockEntity;
import com.uqlism.emoji_deco.text.RichTextParser;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.state.properties.AttachFace;
import org.joml.Matrix4f;

public class GraffitiRenderer implements BlockEntityRenderer<GraffitiBlockEntity> {

    private static final float SURFACE = 100f;          // virtual surface size in font units
    private static final float SCALE   = 1f / SURFACE;  // 100 font units == 1 block
    private static final int   LIGHT   = 0xF000F0;
    private static final int   COLOR   = 0xFFFFFF;

    private final Font font;

    public GraffitiRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.getFont();
    }

    @Override
    public void render(GraffitiBlockEntity be, float partialTicks, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        var state = be.getBlockState();
        if (!(state.getBlock() instanceof GraffitiBlock)) return;

        AttachFace face = state.getValue(GraffitiBlock.FACE);
        Direction facing = state.getValue(GraffitiBlock.FACING);

        pose.pushPose();
        orient(pose, face, facing);
        pose.translate(-SURFACE / 2f, -SURFACE / 2f, 0f);

        Matrix4f matrix = pose.last().pose();
        GraffitiAlignment align = be.getAlignment();
        float lineHeight = SURFACE / GraffitiBlockEntity.MAX_LINES;

        for (int i = 0; i < GraffitiBlockEntity.MAX_LINES; i++) {
            String raw = be.getLine(i);
            if (raw == null || raw.isEmpty()) continue;

            Component parsed = RichTextParser.parse(raw);
            FormattedCharSequence seq = parsed.getVisualOrderText();
            float textWidth = font.width(seq);
            float xPos = switch (align) {
                case LEFT   -> 1f;
                case CENTER -> (SURFACE - textWidth) / 2f;
                case RIGHT  -> SURFACE - textWidth - 1f;
            };
            float yPos = i * lineHeight + (lineHeight - font.lineHeight) / 2f;
            font.drawInBatch(seq, xPos, yPos, COLOR, false, matrix, buffers,
                    Font.DisplayMode.POLYGON_OFFSET, 0, LIGHT);
        }

        pose.popPose();
    }

    private static void orient(PoseStack pose, AttachFace face, Direction facing) {
        pose.translate(0.5, 0.5, 0.5);
        switch (face) {
            case FLOOR -> {
                pose.mulPose(Axis.XP.rotationDegrees(-90));
                pose.mulPose(Axis.ZP.rotationDegrees(rotZ(facing)));
                pose.translate(0, 0, -0.499);
                pose.scale(SCALE, -SCALE, SCALE);
            }
            case CEILING -> {
                pose.mulPose(Axis.XP.rotationDegrees(90));
                pose.mulPose(Axis.ZP.rotationDegrees(-rotZ(facing)));
                pose.translate(0, 0, -0.499);
                pose.scale(SCALE, -SCALE, SCALE);
            }
            default -> {
                pose.mulPose(Axis.YP.rotationDegrees(wallYaw(facing)));
                pose.translate(0, 0, -0.499);
                pose.scale(SCALE, -SCALE, SCALE);
            }
        }
    }

    private static float wallYaw(Direction f) {
        return switch (f) {
            case NORTH -> 180f;
            case SOUTH -> 0f;
            case EAST  -> 90f;
            case WEST  -> -90f;
            default    -> 0f;
        };
    }

    private static float rotZ(Direction f) {
        return switch (f) {
            case NORTH -> 0f;
            case EAST  -> -90f;
            case SOUTH -> 180f;
            case WEST  -> 90f;
            default    -> 0f;
        };
    }
}
