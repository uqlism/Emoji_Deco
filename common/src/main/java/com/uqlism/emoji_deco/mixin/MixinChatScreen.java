package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.client.SuggestionState;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class MixinChatScreen {

    @Shadow(remap = false)
    private EditBox input;

    @Unique
    private SuggestionState runicink$ss() {
        return SuggestionState.of(this);
    }

    @Inject(method = "onEdited", at = @At("HEAD"), remap = false)
    private void runicink$onTyped(String text, CallbackInfo ci) {
        runicink$ss().update(text, input.getCursorPosition());
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$onKeyPressed(int keyCode, int scanCode, int modifiers,
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
                String completed = ss.applyTo(ss.lastInput, entry);
                // insertText で入力全体を置換
                input.setValue(completed);
                int newCursor = ss.pendingCursor;
                if (newCursor >= 0) {
                    input.moveCursorTo(newCursor, false);
                    input.setHighlightPos(newCursor);
                }
                ss.clear();
            }
            cir.setReturnValue(true);
        }
    }
}
