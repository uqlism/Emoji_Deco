package com.uqlism.emoji_deco.client.screen;

import com.uqlism.emoji_deco.block.GraffitiAlignment;
import com.uqlism.emoji_deco.block.GraffitiBlockEntity;
import com.uqlism.emoji_deco.client.CompletionRenderer;
import com.uqlism.emoji_deco.network.GraffitiUpdatePacket;
import com.uqlism.emoji_deco.network.Network;
import com.uqlism.emoji_deco.text.SuggestionState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GraffitiEditScreen extends Screen {

    private final GraffitiBlockEntity target;
    private final EditBox[] boxes = new EditBox[GraffitiBlockEntity.MAX_LINES];
    private GraffitiAlignment alignment;
    private Button alignButton;

    private static final int LINE_HEIGHT = 14;
    private static final int BOX_WIDTH   = 240;

    public static void open(GraffitiBlockEntity be) {
        Minecraft.getInstance().setScreen(new GraffitiEditScreen(be));
    }

    private GraffitiEditScreen(GraffitiBlockEntity be) {
        super(Component.translatable("screen.emoji_deco.graffiti"));
        this.target = be;
        this.alignment = be.getAlignment();
    }

    @Override
    protected void init() {
        int totalHeight = LINE_HEIGHT * GraffitiBlockEntity.MAX_LINES;
        int top = (height - totalHeight) / 2 - 20;
        int left = (width - BOX_WIDTH) / 2;

        for (int i = 0; i < GraffitiBlockEntity.MAX_LINES; i++) {
            final int idx = i;
            EditBox box = new EditBox(font, left, top + i * LINE_HEIGHT, BOX_WIDTH, LINE_HEIGHT - 2,
                    Component.literal("line " + (i + 1)));
            box.setMaxLength(256);
            box.setValue(target.getLine(i));
            box.setResponder(text -> {
                if (boxes[idx] != null && boxes[idx].isFocused()) {
                    SuggestionState.update(text, boxes[idx].getCursorPosition());
                }
            });
            boxes[i] = box;
            addRenderableWidget(box);
        }

        int buttonY = top + totalHeight + 8;
        alignButton = Button.builder(alignmentLabel(), b -> {
            alignment = alignment.next();
            b.setMessage(alignmentLabel());
        }).bounds(left, buttonY, 100, 20).build();
        addRenderableWidget(alignButton);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                b -> save()).bounds(left + BOX_WIDTH - 100, buttonY, 100, 20).build());

        setInitialFocus(boxes[0]);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (SuggestionState.hasSuggestions()) {
            if (keyCode == 265) { // UP
                SuggestionState.moveUp();
                return true;
            }
            if (keyCode == 264) { // DOWN
                SuggestionState.moveDown();
                return true;
            }
            if (keyCode == 258) { // TAB
                applyCompletion();
                return true;
            }
            if (keyCode == 256) { // ESCAPE — dismiss suggestions, don't close screen
                SuggestionState.clear();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void applyCompletion() {
        SuggestionState.Entry entry = SuggestionState.getSelected();
        if (entry == null) return;
        for (EditBox box : boxes) {
            if (box == null || !box.isFocused()) continue;
            String completed = SuggestionState.applyTo(box.getValue(), entry);
            box.setValue(completed);
            int cursor = SuggestionState.pendingCursor;
            if (cursor >= 0) box.moveCursorTo(cursor);
            SuggestionState.clear();
            return;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 30, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderCompletions(graphics);
    }

    private void renderCompletions(GuiGraphics graphics) {
        if (!SuggestionState.hasSuggestions()) return;
        for (EditBox box : boxes) {
            if (box == null || !box.isFocused()) continue;
            // If the focused box's text has drifted (e.g. focus switched), clear and bail
            if (!box.getValue().equals(SuggestionState.lastInput)) {
                SuggestionState.clear();
                return;
            }
            int triggerPos = Math.min(SuggestionState.triggerPos, SuggestionState.lastInput.length());
            int x = box.getX() + 4 + font.width(SuggestionState.lastInput.substring(0, triggerPos));

            int visible     = Math.min(SuggestionState.suggestions.size(), CompletionRenderer.MAX_VISIBLE);
            int completionH = CompletionRenderer.ITEM_HEIGHT * visible;
            // Show below the box; fall back to above if it would clip the bottom
            int y = (box.getY() + box.getHeight() + completionH + 4 <= height)
                    ? box.getY() + box.getHeight() + 1
                    : box.getY() - completionH - 1;

            CompletionRenderer.render(graphics, font,
                    SuggestionState.suggestions, SuggestionState.selectedIndex,
                    x, y, width - 2);
            break;
        }
    }

    private Component alignmentLabel() {
        return Component.translatable("screen.emoji_deco.graffiti.align." + alignment.getSerializedName());
    }

    private void save() {
        String[] lines = new String[GraffitiBlockEntity.MAX_LINES];
        for (int i = 0; i < lines.length; i++) lines[i] = boxes[i].getValue();
        Network.CHANNEL.sendToServer(new GraffitiUpdatePacket(target.getBlockPos(), lines, alignment));
        target.applyUpdate(lines, alignment);
        onClose();
    }

    @Override
    public void onClose() {
        SuggestionState.clear();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
