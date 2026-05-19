package com.uqlism.emoji_deco.client.screen;

import com.uqlism.emoji_deco.block.GraffitiAlignment;
import com.uqlism.emoji_deco.block.GraffitiBlockEntity;
import com.uqlism.emoji_deco.client.CompletionRenderer;
import com.uqlism.emoji_deco.network.GraffitiUpdatePacket;
import com.uqlism.emoji_deco.platform.Services;
import com.uqlism.emoji_deco.client.SuggestionState;
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

    private static final int LINE_HEIGHT = 14;
    private static final int BOX_WIDTH   = 240;
    private static final int PAD_X       = 8;
    private static final int PAD_Y       = 4;

    // Source of truth for line content (survives rebuildWidgets)
    private final String[] lineValues = new String[GraffitiBlockEntity.MAX_LINES];
    // Number of currently visible lines (grows from 1 up to MAX_LINES)
    private int activeLines;
    // Line to focus after the next init()
    private int focusedLine;

    // Computed in init()
    private int panelLeft, panelTop, panelRight, panelBottom;
    private int linesTop;

    // Per-screen suggestion state; registered in REGISTRY via of(this) so
    // ClientEvents.onScreenClose can clear it without knowing this class.
    private final SuggestionState completionState = SuggestionState.of(this);

    public static void open(GraffitiBlockEntity be) {
        Minecraft.getInstance().setScreen(new GraffitiEditScreen(be));
    }

    private GraffitiEditScreen(GraffitiBlockEntity be) {
        super(Component.translatable("screen.emoji_deco.graffiti"));
        this.target    = be;
        this.alignment = be.getAlignment();

        for (int i = 0; i < GraffitiBlockEntity.MAX_LINES; i++) {
            lineValues[i] = be.getLine(i);
        }

        // 保存時の行数を復元し、非空行がそれを超えていれば広げる
        activeLines = be.getDisplayedLines();
        for (int i = GraffitiBlockEntity.MAX_LINES - 1; i >= activeLines; i--) {
            if (!lineValues[i].isEmpty()) { activeLines = i + 1; break; }
        }
        focusedLine = 0;
    }

    @Override
    protected void init() {
        int left   = (width - BOX_WIDTH) / 2;
        int totalH = activeLines * LINE_HEIGHT;
        linesTop   = (height - totalH) / 2; // vertical centre

        panelLeft   = left   - PAD_X;
        panelTop    = linesTop - PAD_Y;
        panelRight  = left   + BOX_WIDTH + PAD_X;
        panelBottom = linesTop + totalH   + PAD_Y;

        for (int i = 0; i < activeLines; i++) {
            final int idx = i;
            int boxY = linesTop + i * LINE_HEIGHT + 2;
            EditBox box = new EditBox(font, left, boxY, BOX_WIDTH, font.lineHeight,
                    Component.literal("line " + (i + 1)));
            box.setMaxLength(GraffitiBlockEntity.MAX_LINE_LENGTH);
            box.setBordered(false);
            box.setValue(lineValues[i]);
            box.setResponder(text -> {
                if (boxes[idx] != null && boxes[idx].isFocused()) {
                    completionState.update(text, boxes[idx].getCursorPosition());
                }
            });
            boxes[i] = box;
            addRenderableWidget(box);
        }
        for (int i = activeLines; i < boxes.length; i++) boxes[i] = null;

        int buttonY = panelBottom + 10;
        addRenderableWidget(Button.builder(alignmentLabel(), b -> {
            alignment = alignment.next();
            b.setMessage(alignmentLabel());
        }).bounds(left, buttonY, 100, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                b -> save()).bounds(left + BOX_WIDTH - 100, buttonY, 100, 20).build());

        setInitialFocus(boxes[Math.min(focusedLine, activeLines - 1)]);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (completionState.hasSuggestions()) {
            if (keyCode == 265) { completionState.moveUp();   return true; }
            if (keyCode == 264) { completionState.moveDown(); return true; }
            if (keyCode == 258) { applyCompletion();          return true; }
            if (keyCode == 256) { completionState.clear();    return true; }
        }

        // Up/Down: move between lines
        if (keyCode == 265) { shiftFocus(-1); return true; }
        if (keyCode == 264) { shiftFocus(+1); return true; }

        // Backspace at position 0 → merge current line into the previous one
        if (keyCode == 259) {
            int focused = getFocusedBoxIndex();
            if (focused > 0 && boxes[focused] != null
                    && boxes[focused].getCursorPosition() == 0) {
                saveBoxValues();
                String prev = lineValues[focused - 1];
                String curr = lineValues[focused];
                lineValues[focused - 1] = prev + curr;          // merge
                for (int i = focused; i < activeLines - 1; i++) lineValues[i] = lineValues[i + 1];
                lineValues[activeLines - 1] = "";
                activeLines--;
                focusedLine = focused - 1;
                rebuildWidgets();
                // Place cursor at the junction (end of what was previously line focused-1)
                if (boxes[focusedLine] != null) {
                    int cur = Math.min(prev.length(), boxes[focusedLine].getValue().length());
                    boxes[focusedLine].moveCursorTo(cur, false);
                    boxes[focusedLine].setHighlightPos(cur);
                }
                return true;
            }
        }

        // Enter / numpad-Enter: split the current line at the cursor
        if (keyCode == 257 || keyCode == 335) {
            boolean shift   = (modifiers & 1) != 0;
            int     focused = getFocusedBoxIndex();
            if (focused < 0) return super.keyPressed(keyCode, scanCode, modifiers);

            if (shift) {
                shiftFocus(-1);
                return true;
            }

            if (activeLines < GraffitiBlockEntity.MAX_LINES) {
                EditBox box    = boxes[focused];
                int     cursor = box.getCursorPosition();
                String  before = box.getValue().substring(0, cursor);
                String  after  = box.getValue().substring(cursor);

                // Persist non-focused boxes
                for (int i = 0; i < activeLines; i++) {
                    if (i != focused && boxes[i] != null) lineValues[i] = boxes[i].getValue();
                }
                // Shift lineValues[focused+1..activeLines-1] → [focused+2..activeLines]
                for (int i = activeLines; i > focused + 1; i--) lineValues[i] = lineValues[i - 1];
                lineValues[focused]     = before;
                lineValues[focused + 1] = after;
                activeLines++;
                focusedLine = focused + 1;
                rebuildWidgets();
                // Move cursor to start of the newly created line
                if (boxes[focusedLine] != null) {
                    boxes[focusedLine].moveCursorTo(0, false);
                    boxes[focusedLine].setHighlightPos(0);
                }
            }
            // else: already at MAX_LINES, silently ignore
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private int getFocusedBoxIndex() {
        for (int i = 0; i < activeLines; i++) {
            if (boxes[i] != null && boxes[i].isFocused()) return i;
        }
        return -1;
    }

    private void shiftFocus(int delta) {
        for (int i = 0; i < activeLines; i++) {
            if (boxes[i] != null && boxes[i].isFocused()) {
                int next = i + delta;
                if (next >= 0 && next < activeLines) {
                    setFocused(boxes[next]);
                    completionState.clear();
                }
                return;
            }
        }
    }

    private void saveBoxValues() {
        for (int i = 0; i < activeLines; i++) {
            if (boxes[i] != null) lineValues[i] = boxes[i].getValue();
        }
    }

    private void applyCompletion() {
        SuggestionState.Entry entry = completionState.getSelected();
        if (entry == null) return;
        for (int i = 0; i < activeLines; i++) {
            EditBox box = boxes[i];
            if (box == null || !box.isFocused()) continue;
            String completed = completionState.applyTo(box.getValue(), entry);
            box.setValue(completed);
            int cursor = completionState.pendingCursor;
            if (cursor >= 0) {
                box.moveCursorTo(cursor, false);
                box.setHighlightPos(cursor);
            }
            completionState.clear();
            return;
        }
    }

    // ── rendering ────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);

        // Panel
        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xFF000000);
        graphics.fill(panelLeft,    panelTop,      panelRight,    panelTop    + 1, 0xFF606060);
        graphics.fill(panelLeft,    panelBottom-1, panelRight,    panelBottom,     0xFF606060);
        graphics.fill(panelLeft,    panelTop,      panelLeft  +1, panelBottom,     0xFF606060);
        graphics.fill(panelRight-1, panelTop,      panelRight,    panelBottom,     0xFF606060);

        // Focused-row highlight
        for (int i = 0; i < activeLines; i++) {
            if (boxes[i] != null && boxes[i].isFocused()) {
                int rowY = linesTop + i * LINE_HEIGHT;
                graphics.fill(panelLeft + 1, rowY, panelRight - 1, rowY + LINE_HEIGHT, 0x40FFFFFF);
                break;
            }
        }

        // Row separators
        for (int i = 1; i < activeLines; i++) {
            int sepY = linesTop + i * LINE_HEIGHT;
            graphics.fill(panelLeft + 1, sepY, panelRight - 1, sepY + 1, 0xFF222222);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        renderCompletions(graphics);
    }

    private void renderCompletions(GuiGraphics graphics) {
        completionState.computeIfReady();
        if (!completionState.hasSuggestions()) return;
        for (int i = 0; i < activeLines; i++) {
            EditBox box = boxes[i];
            if (box == null || !box.isFocused()) continue;
            if (!box.getValue().equals(completionState.lastInput)) {
                completionState.clear();
                return;
            }
            int tp = Math.min(completionState.triggerPos, completionState.lastInput.length());
            int x  = box.getX() + font.width(completionState.lastInput.substring(0, tp));

            int visible     = Math.min(completionState.suggestions.size(), CompletionRenderer.MAX_VISIBLE);
            int completionH = CompletionRenderer.ITEM_HEIGHT * visible;
            int y = (box.getY() + box.getHeight() + completionH + 4 <= height)
                    ? box.getY() + box.getHeight() + 1
                    : box.getY() - completionH - 1;

            CompletionRenderer.render(graphics, font,
                    completionState.suggestions, completionState.selectedIndex,
                    x, y, width - 2);
            break;
        }
    }

    // ── misc ─────────────────────────────────────────────────────────────────

    private Component alignmentLabel() {
        return Component.translatable("screen.emoji_deco.graffiti.align." + alignment.getSerializedName());
    }

    private void save() {
        saveBoxValues();
        for (int i = activeLines; i < lineValues.length; i++) lineValues[i] = "";
        Services.network().sendGraffitiUpdate(new GraffitiUpdatePacket(target.getBlockPos(), lineValues, alignment, activeLines));
        target.applyUpdate(lineValues, alignment, activeLines);
        onClose();
    }

    @Override
    public void onClose() {
        completionState.clear();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
