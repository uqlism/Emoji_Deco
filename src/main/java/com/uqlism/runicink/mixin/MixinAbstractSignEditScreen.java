package com.uqlism.runicink.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSignEditScreen.class)
public class MixinAbstractSignEditScreen {

    // SRG: m_279811_ = lambda$init$4(String) — validates whether a line fits within sign width
    @Inject(method = "m_279811_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$noWidthLimit(String text, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
