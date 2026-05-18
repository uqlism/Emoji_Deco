package com.uqlism.emoji_deco.network;

import com.uqlism.emoji_deco.block.GraffitiAlignment;
import com.uqlism.emoji_deco.block.GraffitiBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record GraffitiUpdatePacket(BlockPos pos, String[] lines, GraffitiAlignment alignment, int displayedLines)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GraffitiUpdatePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("emoji_deco", "graffiti_update"));

    public static final StreamCodec<FriendlyByteBuf, GraffitiUpdatePacket> STREAM_CODEC =
            StreamCodec.of(GraffitiUpdatePacket::encode, GraffitiUpdatePacket::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(FriendlyByteBuf buf, GraffitiUpdatePacket pkt) {
        buf.writeBlockPos(pkt.pos);
        buf.writeByte(pkt.alignment.ordinal());
        buf.writeVarInt(pkt.displayedLines);
        buf.writeVarInt(pkt.lines.length);
        for (String line : pkt.lines) buf.writeUtf(line == null ? "" : line, 1024);
    }

    public static GraffitiUpdatePacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        GraffitiAlignment[] alignments = GraffitiAlignment.values();
        int ordinal = Math.max(0, Math.min(buf.readByte(), alignments.length - 1));
        GraffitiAlignment align = alignments[ordinal];
        int displayedLines = buf.readVarInt();
        int n = buf.readVarInt();
        String[] lines = new String[Math.min(n, GraffitiBlockEntity.MAX_LINES)];
        for (int i = 0; i < n; i++) {
            String s = buf.readUtf(1024);
            if (i < lines.length) lines[i] = s;
        }
        return new GraffitiUpdatePacket(pos, lines, align, displayedLines);
    }

    public static void handle(GraffitiUpdatePacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            Level level = player.level();
            if (!level.isLoaded(pkt.pos)) return;
            if (player.distanceToSqr(pkt.pos.getX() + 0.5, pkt.pos.getY() + 0.5, pkt.pos.getZ() + 0.5) > 64.0) return;
            BlockEntity be = level.getBlockEntity(pkt.pos);
            if (be instanceof GraffitiBlockEntity g) {
                g.applyUpdate(pkt.lines, pkt.alignment, pkt.displayedLines);
            }
        });
    }
}
