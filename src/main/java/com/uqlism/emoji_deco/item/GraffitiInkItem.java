package com.uqlism.emoji_deco.item;

import com.uqlism.emoji_deco.Registration;
import com.uqlism.emoji_deco.block.GraffitiBlock;
import com.uqlism.emoji_deco.block.GraffitiBlockEntity;
import com.uqlism.emoji_deco.client.screen.GraffitiEditScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GraffitiInkItem extends Item {

    public GraffitiInkItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        BlockPlaceContext bpc = new BlockPlaceContext(context);
        GraffitiBlock graffitiBlock = (GraffitiBlock) Registration.GRAFFITI_BLOCK.get();
        BlockState stateToPlace = graffitiBlock.getStateForPlacement(bpc);
        if (stateToPlace == null) return InteractionResult.FAIL;

        BlockPos placePos = bpc.getClickedPos();
        if (!level.getBlockState(placePos).canBeReplaced(bpc)) return InteractionResult.FAIL;

        level.setBlock(placePos, stateToPlace, Block.UPDATE_ALL);

        if (level.isClientSide) {
            BlockEntity be = level.getBlockEntity(placePos);
            if (be instanceof GraffitiBlockEntity graffiti) {
                GraffitiEditScreen.open(graffiti);
            }
        } else {
            if (player != null && !player.isCreative()) {
                context.getItemInHand().hurtAndBreak(1, player,
                        p -> p.broadcastBreakEvent(context.getHand()));
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
