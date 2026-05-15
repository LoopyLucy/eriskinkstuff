package co.uk.loopylucy.kinkstuff.block.blocks;

import co.uk.loopylucy.kinkstuff.block.entity.PetBedBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PetBedBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<PetBedBlock> CODEC = simpleCodec(PetBedBlock::new);

    public static final IntegerProperty X_PART = IntegerProperty.create("x_part", 0, 1);
    public static final IntegerProperty Z_PART = IntegerProperty.create("z_part", 0, 1);
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;

    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 3.0D, 16.0D);

    public PetBedBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(X_PART, 0)
                .setValue(Z_PART, 0)
                .setValue(OCCUPIED, false));
    }

    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        super.tick(state, level, pos, random);

        if (state.getValue(OCCUPIED)) {
            // Verify if a sleeping player entity is still resting on this block coordinate
            boolean playerPresent = !level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(pos)).isEmpty();
            if (!playerPresent) {
                level.setBlock(pos, state.setValue(OCCUPIED, false), 3);
            }
        }
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull Optional<ServerPlayer.RespawnPosAngle> getRespawnPosition(BlockState state, EntityType<?> type, LevelReader levelReader, BlockPos pos, float orientation) {
        Vec3 spawnBlock = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.1875D, pos.getZ() + 0.5D);
        return Optional.of(ServerPlayer.RespawnPosAngle.of(
                spawnBlock,
                pos
        ));
    }

    @Override
    public boolean isBed(BlockState state, BlockGetter level, BlockPos pos, @Nullable LivingEntity player) {
        return true;
    }

    @Override
    public Direction getBedDirection(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return state.getValue(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.CONSUME;

        // Explode if clicked in an invalid sleeping dimension (like the Nether)
        if (!level.dimensionType().natural()) {
            level.explode(null, pos.getX(), pos.getY(), pos.getZ(), 5.0F, Level.ExplosionInteraction.BLOCK);
            return InteractionResult.SUCCESS;
        }

        Direction facing = state.getValue(FACING);
        int currentX = state.getValue(X_PART);
        int currentZ = state.getValue(Z_PART);

        // Find bottom-left grid origin (0,0) and locate the correct target pillow side block position
        BlockPos baseOrigin = pos.subtract(translateGridOffset(BlockPos.ZERO, currentX, currentZ, facing));
        BlockPos leftPillowPos = baseOrigin;
        BlockPos gridShift = translateGridOffset(BlockPos.ZERO, 1, 0, facing);
        BlockPos rightPillowPos = baseOrigin.offset(gridShift.getX(), gridShift.getY(), gridShift.getZ());

        BlockPos targetPillowPos = (currentX == 0) ? leftPillowPos : rightPillowPos;
        BlockState targetPillowState = level.getBlockState(targetPillowPos);

        if (targetPillowState.getBlock() != this) return InteractionResult.FAIL;

        // 1. DAYTIME SPAWN SETTING ENHANCEMENT
        // If it is daytime, update the player's spawn point immediately without putting them to sleep
        if (!level.isNight() && !level.isThundering()) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                // Sets the respawn position on the clicked side of the bed, keeping the angle looking direction intact
                serverPlayer.setRespawnPosition(level.dimension(), targetPillowPos, player.getYRot(), false, true);
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("block.minecraft.bed.nospawn"), true);
            }
            return InteractionResult.SUCCESS;
        }

        // 2. NIGHTTIME SLEEP ROUTINE (Existing verified loop)
        if (targetPillowState.getValue(OCCUPIED)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("block.minecraft.bed.occupied"), true);
            return InteractionResult.SUCCESS;
        }

        player.startSleepInBed(targetPillowPos).ifLeft(problem -> {
            if (problem.getMessage() != null) {
                player.displayClientMessage(problem.getMessage(), true);
            }
        });

        if (player.isSleeping()) {
            level.setBlock(targetPillowPos, targetPillowState.setValue(OCCUPIED, true), 3);
            Vec3 anchor = new Vec3(targetPillowPos.getX() + 0.5D, targetPillowPos.getY() + 0.1875D, targetPillowPos.getZ() + 0.5D);
            player.setPos(anchor);
            player.setPose(Pose.SLEEPING);
        }

        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockPos basePos = context.getClickedPos();
        Level level = context.getLevel();

        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                BlockPos targetPos = translateGridOffset(basePos, x, z, facing);
                if (!level.getBlockState(targetPos).canBeReplaced(context)) {
                    return null;
                }
            }
        }
        return this.defaultBlockState().setValue(FACING, facing).setValue(X_PART, 0).setValue(Z_PART, 0).setValue(OCCUPIED, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;

        DyedItemColor dyedColor = stack.get(DataComponents.DYED_COLOR);

        if (level.getBlockEntity(pos) instanceof PetBedBlockEntity bedEntity) {
            if (dyedColor != null) {
                bedEntity.setCustomColour(dyedColor.rgb());
            } else {
                bedEntity.setCustomColour(0xFFFFFFFF);
            }
        }

        Direction facing = state.getValue(FACING);
        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos targetPos = translateGridOffset(pos, x, z, facing);
                level.setBlock(targetPos, state.setValue(X_PART, x).setValue(Z_PART, z).setValue(OCCUPIED, false), 3);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide()) {
                Direction facing = state.getValue(FACING);
                int currentX = state.getValue(X_PART);
                int currentZ = state.getValue(Z_PART);

                BlockPos originPos = pos.subtract(translateGridOffset(BlockPos.ZERO, currentX, currentZ, facing));

                BlockPos tl = originPos;
                BlockPos tr = originPos.offset(translateGridOffset(BlockPos.ZERO, 1, 0, facing));
                BlockPos bl = originPos.offset(translateGridOffset(BlockPos.ZERO, 0, 1, facing));
                BlockPos br = originPos.offset(translateGridOffset(BlockPos.ZERO, 1, 1, facing));

                // FIX: The master origin block (tl) MUST be allowed to drop its items (true)
                // This guarantees the loot table executes and drops your pet bed item!
                if (level.getBlockState(tl).is(this) && !tl.equals(pos)) {
                    level.destroyBlock(tl, true);
                }

                // The other three blocks keep drops disabled (false) so they don't duplicate the item
                if (level.getBlockState(tr).is(this) && !tr.equals(pos)) level.destroyBlock(tr, false);
                if (level.getBlockState(bl).is(this) && !bl.equals(pos)) level.destroyBlock(bl, false);
                if (level.getBlockState(br).is(this) && !br.equals(pos)) level.destroyBlock(br, false);
            }
        } else {
            if (state.getValue(OCCUPIED) && !newState.getValue(OCCUPIED) && !level.isClientSide) {
                level.blockUpdated(pos, this);
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        BlockPos originPos = currentPos.subtract(translateGridOffset(BlockPos.ZERO, state.getValue(X_PART), state.getValue(Z_PART), facing));

        // Safety rail fallback loop
        if (level.getBlockState(originPos).getBlock() != this) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    private BlockPos translateGridOffset(BlockPos startPos, int gridX, int gridZ, Direction facing) {
        return switch (facing) {
            case NORTH -> startPos.east(gridX).south(gridZ);
            case SOUTH -> startPos.west(gridX).north(gridZ);
            case WEST  -> startPos.north(gridX).east(gridZ);
            case EAST  -> startPos.south(gridX).west(gridZ);
            default    -> startPos;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, X_PART, Z_PART, OCCUPIED);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        if (blockState.getValue(X_PART) == 0 && blockState.getValue(Z_PART) == 0) {
            return new PetBedBlockEntity(blockPos, blockState);
        }
        return null;
    }
}
