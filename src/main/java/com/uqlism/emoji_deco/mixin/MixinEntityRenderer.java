package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.Config;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
        // オリジナルをそのまま返す。MixinFont.drawInBatch(Component) が
        // ComponentSequenceConverter.computeNow() で変換する（scale/glow 対応）。
        if (!Config.enableEntityNames) return entity.getDisplayName();
        return entity.getDisplayName();
    }
}
