package com.uqlism.runicink.mixin;

import com.uqlism.runicink.text.RichTextParser;
import com.uqlism.runicink.text.ShortcodeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.uqlism.runicink.text.ChatInputState;
import com.uqlism.runicink.text.ChatSuggestionState;

@Mixin(ChatScreen.class)
public class MixinChatScreen {

    // 入力変更時
    @Inject(method = "m_95610_", at = @At("HEAD"), remap = false)
    private void runicink$onTyped(String text, CallbackInfo ci) {
        ChatSuggestionState.update(text);
    }

    @Inject(method = "m_7933_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (ChatSuggestionState.suggestions.isEmpty()) return;

        if (keyCode == 265) {
            ChatSuggestionState.moveUp();
            cir.setReturnValue(true);
        } else if (keyCode == 264) {
            ChatSuggestionState.moveDown();
            cir.setReturnValue(true);
        } else if (keyCode == 258) {
            String selected = ChatSuggestionState.getSelected();
            if (selected != null) {
                String current = ChatSuggestionState.lastInput;
                int lastColon = current.lastIndexOf(':');
                String completed = current.substring(0, lastColon + 1) + selected + ":";
                try {
                    // m_95612_ = setChatLine(String)
                    java.lang.reflect.Method m = ChatScreen.class.getDeclaredMethod("m_95612_", String.class);
                    m.setAccessible(true);
                    m.invoke((ChatScreen)(Object)this, completed);
                } catch (Exception e) {
                    // fallback: EditBox.setValue を直接呼ぶ
                }
                cir.setReturnValue(true);
            }
        }
    }
}