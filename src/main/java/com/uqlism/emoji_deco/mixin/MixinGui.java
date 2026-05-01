package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.Config;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gui.class)
public abstract class MixinGui {

    // タイトル・サブタイトル・アクションバーはオリジナルをそのまま格納し、
    // 描画時に MixinFont.drawInBatch(Component) → ComponentSequenceConverter が処理する。

    @ModifyVariable(method = "m_168714_", at = @At("HEAD"), argsOnly = true, remap = false)
    private Component runicink$transformTitle(Component component) {
        return component;
    }

    @ModifyVariable(method = "m_168711_", at = @At("HEAD"), argsOnly = true, remap = false)
    private Component runicink$transformSubtitle(Component component) {
        return component;
    }

    @ModifyVariable(method = "m_93063_", at = @At("HEAD"), argsOnly = true, remap = false)
    private Component runicink$transformOverlay(Component component) {
        return component;
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
        return stack.getHoverName();
    }
}
