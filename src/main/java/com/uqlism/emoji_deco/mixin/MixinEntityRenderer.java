package com.uqlism.emoji_deco.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.uqlism.emoji_deco.Config;
import com.uqlism.emoji_deco.text.parse.RichTextParser;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Redirect(
        method = "m_7392_",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;m_5446_()Lnet/minecraft/network/chat/Component;"
        ),
        remap = false,
        require = 0
    )
    private Component runicink$parseEntityName(Entity entity) {
        Component original = entity.getDisplayName();
        if (!Config.enableEntityNames) return original;
        // Only process entities that have an explicit custom name (set with name tag)
        Component customName = entity.getCustomName();
        if (customName == null) return original;
        String raw = customName.getString();
        if (raw.isBlank()) return original;
        return RichTextParser.parse(raw).toComponent();
    }
}
