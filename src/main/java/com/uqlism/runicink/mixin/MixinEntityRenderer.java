package com.uqlism.runicink.mixin;

import com.uqlism.runicink.Config;
import com.uqlism.runicink.text.RichTextParser;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getDisplayName()Lnet/minecraft/network/chat/Component;"
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
        return RichTextParser.parse(raw);
    }
}
