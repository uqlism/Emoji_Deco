package com.uqlism.emoji_deco.block;

import com.uqlism.emoji_deco.Registration;
import com.uqlism.emoji_deco.text.ir.RichNode;
import com.uqlism.emoji_deco.text.parse.RichTextParser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GraffitiBlockEntity extends BlockEntity {
    public static final int MAX_LINES       = 10;
    public static final int MAX_LINE_LENGTH = 256;

    private final String[] lines = new String[MAX_LINES];
    private GraffitiAlignment alignment = GraffitiAlignment.LEFT;
    private int displayedLines = 1;

    public GraffitiBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.GRAFFITI_BLOCK_ENTITY.get(), pos, state);
        for (int i = 0; i < MAX_LINES; i++) lines[i] = "";
    }

    public String  getLine(int index)         { return (index < 0 || index >= MAX_LINES) ? "" : lines[index]; }
    public void    setLine(int index, String t){ if (index >= 0 && index < MAX_LINES) lines[index] = t == null ? "" : t; }
    public String[] getLines()                 { return lines; }
    public GraffitiAlignment getAlignment()    { return alignment; }
    public void    setAlignment(GraffitiAlignment a) { this.alignment = a; }
    public int     getDisplayedLines()         { return displayedLines; }

    /**
     * Returns RichNodes for each displayed line.
     * RichTextParser.parse() goes through PARSE_CACHE + CACHED_NODE, so
     * repeated calls with the same text hit the cache; no per-block state needed.
     * Call only on the client (render) thread.
     */
    public RichNode[] getRichLines() {
        RichNode[] result = new RichNode[displayedLines];
        for (int i = 0; i < displayedLines; i++) {
            String raw = lines[i];
            result[i] = (raw != null && !raw.isEmpty())
                    ? RichTextParser.parse(raw) : RichNode.empty();
        }
        return result;
    }

    private void invalidateCache() {}

    public void applyUpdate(String[] newLines, GraffitiAlignment newAlignment, int newDisplayedLines) {
        for (int i = 0; i < MAX_LINES; i++) {
            String v = (i < newLines.length && newLines[i] != null) ? newLines[i] : "";
            lines[i] = v.length() > MAX_LINE_LENGTH ? v.substring(0, MAX_LINE_LENGTH) : v;
        }
        this.alignment     = newAlignment == null ? GraffitiAlignment.LEFT : newAlignment;
        this.displayedLines = Math.max(1, Math.min(newDisplayedLines, MAX_LINES));
        invalidateCache();
        setChanged();
        if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (int i = 0; i < MAX_LINES; i++) tag.putString("line" + i, lines[i]);
        tag.putString("align", alignment.getSerializedName());
        tag.putInt("displayedLines", displayedLines);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < MAX_LINES; i++)
            lines[i] = tag.contains("line" + i) ? tag.getString("line" + i) : "";
        alignment = GraffitiAlignment.byName(tag.getString("align"));
        invalidateCache();
        if (tag.contains("displayedLines")) {
            displayedLines = Math.max(1, Math.min(tag.getInt("displayedLines"), MAX_LINES));
        } else {
            displayedLines = 1;
            for (int i = MAX_LINES - 1; i >= 0; i--) {
                if (!lines[i].isEmpty()) { displayedLines = i + 1; break; }
            }
        }
    }

    @Override public CompoundTag getUpdateTag() { CompoundTag t = super.getUpdateTag(); saveAdditional(t); return t; }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) load(pkt.getTag());
    }
}
