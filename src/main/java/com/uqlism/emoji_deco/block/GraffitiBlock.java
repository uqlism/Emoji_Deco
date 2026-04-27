package com.uqlism.emoji_deco.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class GraffitiBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {

    public static final SoundType GRAFFITI_SOUND_TYPE = new SoundType(
            0.8f, 1.8f,
            SoundEvents.SLIME_SQUISH_SMALL,
            SoundEvents.SLIME_SQUISH_SMALL,
            SoundEvents.SLIME_SQUISH_SMALL,
            SoundEvents.SLIME_SQUISH_SMALL,
            SoundEvents.SLIME_SQUISH_SMALL);

    private static final VoxelShape SHAPE_FLOOR   = Block.box(0, 0, 0, 16, 0.5, 16);
    private static final VoxelShape SHAPE_CEILING = Block.box(0, 15.5, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_WALL_N  = Block.box(0, 0, 15.5, 16, 16, 16);
    private static final VoxelShape SHAPE_WALL_S  = Block.box(0, 0, 0, 16, 16, 0.5);
    private static final VoxelShape SHAPE_WALL_E  = Block.box(0, 0, 0, 0.5, 16, 16);
    private static final VoxelShape SHAPE_WALL_W  = Block.box(15.5, 0, 0, 16, 16, 16);

    public GraffitiBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACE, AttachFace.WALL)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        AttachFace face = state.getValue(FACE);
        if (face == AttachFace.FLOOR) return SHAPE_FLOOR;
        if (face == AttachFace.CEILING) return SHAPE_CEILING;
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_WALL_N;
            case SOUTH -> SHAPE_WALL_S;
            case EAST  -> SHAPE_WALL_E;
            case WEST  -> SHAPE_WALL_W;
            default    -> Shapes.empty();
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction attached = getConnectedDirection(state).getOpposite();
        BlockPos supportPos = pos.relative(attached);
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, attached.getOpposite());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return canSurvive(state, level, pos) ? state : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction clicked = ctx.getClickedFace();
        BlockState state;
        if (clicked == Direction.UP) {
            state = defaultBlockState().setValue(FACE, AttachFace.FLOOR)
                    .setValue(FACING, ctx.getHorizontalDirection());
        } else if (clicked == Direction.DOWN) {
            state = defaultBlockState().setValue(FACE, AttachFace.CEILING)
                    .setValue(FACING, ctx.getHorizontalDirection());
        } else {
            state = defaultBlockState().setValue(FACE, AttachFace.WALL)
                    .setValue(FACING, clicked);
        }
        return canSurvive(state, ctx.getLevel(), ctx.getClickedPos()) ? state : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof GraffitiBlockEntity graffiti) {
                com.uqlism.emoji_deco.client.screen.GraffitiEditScreen.open(graffiti);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target,
            BlockGetter level, BlockPos pos, Player player) {
        // Return the actual stack from inventory so the damage value matches for pick-block comparison
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack slot = inv.getItem(i);
            if (slot.getItem() instanceof com.uqlism.emoji_deco.item.GraffitiInkItem) {
                return slot.copy();
            }
        }
        return new ItemStack(com.uqlism.emoji_deco.Registration.GRAFFITI_INK_ITEM.get());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GraffitiBlockEntity(pos, state);
    }
}
