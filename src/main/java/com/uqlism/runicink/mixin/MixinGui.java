package com.uqlism.runicink.mixin;

import com.uqlism.runicink.text.ComponentTransformer;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

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
}
