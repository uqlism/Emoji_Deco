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

    // SRG: f_95573_ = f_95573_ (EditBox)
    @Shadow(remap = false)
    private EditBox f_95573_;

    @Unique
    private SuggestionState runicink$ss() {
        return SuggestionState.of(this);
    }

    // SRG: m_95610_ = onEdited(String)
    @Inject(method = "m_95610_", at = @At("HEAD"), remap = false)
    private void runicink$onTyped(String text, CallbackInfo ci) {
        runicink$ss().update(text, f_95573_.getCursorPosition());
    }

    // SRG: m_7933_ = keyPressed(int, int, int)
    @Inject(method = "m_7933_", at = @At("HEAD"), cancellable = true, remap = false)
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
                try {
                    // m_95612_ = setChatLine(String)
                    java.lang.reflect.Method m =
                            ChatScreen.class.getDeclaredMethod("m_95612_", String.class);
                    m.setAccessible(true);
                    m.invoke((ChatScreen) (Object) this, completed);
                } catch (Exception ignored) {}

                int newCursor = ss.pendingCursor;
                if (newCursor >= 0) {
                    f_95573_.moveCursorTo(newCursor, false);
                    f_95573_.setHighlightPos(newCursor);
                }
                ss.clear();
            }
            cir.setReturnValue(true);
        }
    }
}
