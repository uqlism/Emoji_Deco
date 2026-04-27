package com.uqlism.emoji_deco.block;

import com.uqlism.emoji_deco.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GraffitiBlockEntity extends BlockEntity {
    public static final int MAX_LINES = 10;

    private final String[] lines = new String[MAX_LINES];
    private GraffitiAlignment alignment = GraffitiAlignment.LEFT;
    private int displayedLines = 1;

    public GraffitiBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.GRAFFITI_BLOCK_ENTITY.get(), pos, state);
        for (int i = 0; i < MAX_LINES; i++) lines[i] = "";
    }

    public String getLine(int index) {
        return (index < 0 || index >= MAX_LINES) ? "" : lines[index];
    }

    public void setLine(int index, String text) {
        if (index < 0 || index >= MAX_LINES) return;
        lines[index] = text == null ? "" : text;
    }

    public String[] getLines() {
        return lines;
    }

    public GraffitiAlignment getAlignment() {
        return alignment;
    }

    public void setAlignment(GraffitiAlignment alignment) {
        this.alignment = alignment;
    }

    public int getDisplayedLines() {
        return displayedLines;
    }

    public void applyUpdate(String[] newLines, GraffitiAlignment newAlignment, int newDisplayedLines) {
        for (int i = 0; i < MAX_LINES; i++) {
            lines[i] = (i < newLines.length && newLines[i] != null) ? newLines[i] : "";
        }
        this.alignment = newAlignment == null ? GraffitiAlignment.LEFT : newAlignment;
        this.displayedLines = Math.max(1, Math.min(newDisplayedLines, MAX_LINES));
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (int i = 0; i < MAX_LINES; i++) {
            tag.putString("line" + i, lines[i]);
        }
        tag.putString("align", alignment.getSerializedName());
        tag.putInt("displayedLines", displayedLines);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < MAX_LINES; i++) {
            lines[i] = tag.contains("line" + i) ? tag.getString("line" + i) : "";
        }
        alignment = GraffitiAlignment.byName(tag.getString("align"));
        if (tag.contains("displayedLines")) {
            displayedLines = Math.max(1, Math.min(tag.getInt("displayedLines"), MAX_LINES));
        } else {
            // 旧データ: 最後の非空行から推定
            displayedLines = 1;
            for (int i = MAX_LINES - 1; i >= 0; i--) {
                if (!lines[i].isEmpty()) { displayedLines = i + 1; break; }
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) load(pkt.getTag());
    }
}
