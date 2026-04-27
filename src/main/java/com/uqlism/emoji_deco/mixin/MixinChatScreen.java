package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.text.SuggestionState;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class MixinChatScreen {

    // SRG: m_95610_ = onEdited(String)
    @Inject(method = "m_95610_", at = @At("HEAD"), remap = false)
    private void runicink$onTyped(String text, CallbackInfo ci) {
        SuggestionState.update(text);
    }

    // SRG: m_7933_ = keyPressed(int, int, int)
    @Inject(method = "m_7933_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$onKeyPressed(int keyCode, int scanCode, int modifiers,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (!SuggestionState.hasSuggestions()) return;

        if (keyCode == 265) {           // UP
            SuggestionState.moveUp();
            cir.setReturnValue(true);
        } else if (keyCode == 264) {    // DOWN
            SuggestionState.moveDown();
            cir.setReturnValue(true);
        } else if (keyCode == 258) {    // TAB
            SuggestionState.Entry entry = SuggestionState.getSelected();
            if (entry != null) {
                String completed = SuggestionState.applyTo(SuggestionState.lastInput, entry);
                try {
                    // m_95612_ = setChatLine(String)
                    java.lang.reflect.Method m =
                            ChatScreen.class.getDeclaredMethod("m_95612_", String.class);
                    m.setAccessible(true);
                    m.invoke((ChatScreen) (Object) this, completed);
                } catch (Exception ignored) {}
                SuggestionState.clear();
            }
            cir.setReturnValue(true);
        }
    }
}
