package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.client.SuggestionState;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSignEditScreen.class)
public abstract class MixinAbstractSignEditScreen {

    @Shadow(remap = false) private String[] messages;
    @Shadow(remap = false) private int line;
    @Shadow(remap = false) @Nullable private TextFieldHelper signField;

    @Shadow(remap = false)
    protected abstract void setMessage(String message);

    @Unique
    private SuggestionState runicink$ss() {
        return SuggestionState.of(this);
    }

    // Width limit bypass — lambda$init$4(String)
    @Inject(method = "lambda$init$4", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$noWidthLimit(String text, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    @Inject(method = "charTyped", at = @At("RETURN"), remap = false)
    private void runicink$onCharTyped(char c, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        String msg = messages[line];
        int cursor = signField != null ? signField.getCursorPos() : msg.length();
        runicink$ss().update(msg, cursor);
    }

    // HEAD: intercept UP/DOWN/TAB to navigate/apply suggestions
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$onKeyPressedHead(int keyCode, int scanCode, int modifiers,
                                            CallbackInfoReturnable<Boolean> cir) {
        SuggestionState ss = runicink$ss();
        if (!ss.hasSuggestions()) return;

        if (keyCode == 265) {           // UP
            ss.moveUp();
            cir.setReturnValue(true);
        } else if (keyCode == 264) {    // DOWN
            ss.moveDown();
            cir.setReturnValue(true);
        } else if (keyCode == 258) {    // TAB
            SuggestionState.Entry entry = ss.getSelected();
            if (entry != null) {
                String completed = ss.applyTo(messages[line], entry);
                int newCursor = ss.pendingCursor;
                setMessage(completed);
                if (newCursor >= 0 && signField != null) {
                    signField.setCursorPos(newCursor, false);
                }
                ss.clear();
            }
            cir.setReturnValue(true);
        }
    }

    // RETURN: refresh suggestions after key presses
    @Inject(method = "keyPressed", at = @At("RETURN"), remap = false)
    private void runicink$onKeyPressedReturn(int keyCode, int scanCode, int modifiers,
                                              CallbackInfoReturnable<Boolean> cir) {
        SuggestionState ss = runicink$ss();
        if (ss.hasSuggestions() && (keyCode == 265 || keyCode == 264 || keyCode == 258)) return;
        String msg = messages[line];
        int cursor = signField != null ? signField.getCursorPos() : msg.length();
        ss.update(msg, cursor);
    }
}
