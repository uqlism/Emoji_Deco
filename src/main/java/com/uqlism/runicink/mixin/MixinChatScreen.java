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

@Mixin(ChatScreen.class)
public class MixinChatScreen {

@Inject(method = "m_95610_", at = @At("HEAD"), remap = false)
private void runicink$onEdited(String text, CallbackInfo ci) {
    System.out.println("[RunicInk] m_95610_ called: " + text);
    ChatInputState.lastInput = text;
}
}