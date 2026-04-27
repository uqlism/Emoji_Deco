package com.uqlism.emoji_deco.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.uqlism.emoji_deco.Config;
import com.uqlism.emoji_deco.text.ComponentTransformer;
import com.uqlism.emoji_deco.text.RichTextParser;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

@Mixin(Gui.class)
public abstract class MixinGui {

    // SRG: m_168714_ -> setTitle(Component)
    @ModifyVariable(method = "m_168714_", at = @At("HEAD"), argsOnly = true, remap = false)
    private Component runicink$transformTitle(Component component) {
        return ComponentTransformer.transform(component);
    }

    // SRG: m_168711_ -> setSubtitle(Component)
    @ModifyVariable(method = "m_168711_", at = @At("HEAD"), argsOnly = true, remap = false)
    private Component runicink$transformSubtitle(Component component) {
        return ComponentTransformer.transform(component);
    }

    // SRG: m_93063_ -> setOverlayMessage(Component, boolean)  (action bar)
    @ModifyVariable(method = "m_93063_", at = @At("HEAD"), argsOnly = true, remap = false)
    private Component runicink$transformOverlay(Component component) {
        return ComponentTransformer.transform(component);
    }

@Redirect(
    method = "renderSelectedItemName",
    at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;m_41786_()Lnet/minecraft/network/chat/Component;"
    ),
    remap = false,
    require = 1
)
private Component runicink$transformHotbarName(ItemStack stack) {
    Component original = stack.getHoverName();
    if (!Config.enableItemNames) return original;
    String raw = original.getString();
    if (raw.isBlank()) return original;
    return RichTextParser.parse(raw);
}
}
