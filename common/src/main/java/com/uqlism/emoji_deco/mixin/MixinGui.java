package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.client.GuiTextContext;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ホットバーのアイテム名描画中を MixinGuiGraphics に伝える (enableItemNames / enableGui の振り分け用)。 */
@Mixin(Gui.class)
public class MixinGui {

    @Inject(method = {"renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;)V", "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;I)V"}, at = @At("HEAD"), require = 0, remap = false)
    private void runicink$enterItemName(CallbackInfo ci) {
        GuiTextContext.setInHotbarItemName(true);
    }

    @Inject(method = {"renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;)V", "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;I)V"}, at = @At("RETURN"), require = 0, remap = false)
    private void runicink$exitItemName(CallbackInfo ci) {
        GuiTextContext.setInHotbarItemName(false);
    }
}
