/*
 * Copyright 2023 FrozenBlock
 * This file is part of Wilder Wild.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */

package net.frozenblock.wilderwild.block;

import net.frozenblock.wilderwild.registry.RegisterProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShelfFungusBlock extends FaceAttachedHorizontalDirectionalBlock implements SimpleWaterloggedBlock, BonemealableBlock {
	public static final IntegerProperty AGE = BlockStateProperties.AGE_2;
	public static final IntegerProperty STAGE = RegisterProperties.FUNGUS_STAGE;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	protected static final VoxelShape NORTH_WALL_SHAPE = Block.box(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D);
	protected static final VoxelShape SOUTH_WALL_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D);
	protected static final VoxelShape WEST_WALL_SHAPE = Block.box(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
	protected static final VoxelShape EAST_WALL_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D);
	protected static final VoxelShape FLOOR_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 3.0D, 16.0D);
	protected static final VoxelShape CEILING_SHAPE = Block.box(0.0D, 13.0D, 0.0D, 16.0D, 16.0D, 16.0D);

	public ShelfFungusBlock(@NotNull Properties settings) {
		super(settings);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false).setValue(FACE, AttachFace.WALL).setValue(STAGE, 1));
	}

	@NotNull
	public static AttachFace getFace(@NotNull Direction direction) {
		if (direction.getAxis() == Direction.Axis.Y) {
			return direction == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR;
		}
		return AttachFace.WALL;
	}

	private static boolean isFullyGrown(@NotNull BlockState state) {
		return state.getValue(STAGE) == 4;
	}

	@Override
	@NotNull
	public InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
		ItemStack itemStack = player.getItemInHand(hand);
		int i = state.getValue(STAGE);
		if (i > 1 && itemStack.is(Items.SHEARS)) {
			popResource(level, pos, new ItemStack(state.getBlock().asItem()));
			level.setBlockAndUpdate(pos, state.setValue(STAGE, i - 1));
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0F, 1.0F);
			itemStack.hurtAndBreak(1, player, (playerx) -> playerx.broadcastBreakEvent(hand));
			level.gameEvent(player, GameEvent.SHEAR, pos);
			return InteractionResult.sidedSuccess(level.isClientSide);
		} else {
			return super.use(state, level, pos, player, hand, hit);
		}
	}

	@Override
	protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACE, FACING, AGE, STAGE, WATERLOGGED);
	}

	@Override
	public boolean canBeReplaced(@NotNull BlockState state, @NotNull BlockPlaceContext context) {
		return !context.isSecondaryUseActive() && context.getItemInHand().is(this.asItem()) && state.getValue(STAGE) < 4 || super.canBeReplaced(state, context);
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
		BlockState insideState = context.getLevel().getBlockState(context.getClickedPos());
		if (insideState.is(this)) {
			return insideState.setValue(STAGE, Math.min(4, insideState.getValue(STAGE) + 1));
		}
		boolean waterlogged = insideState.hasProperty(BlockStateProperties.WATERLOGGED) ? insideState.getValue(BlockStateProperties.WATERLOGGED) : false;
		if (!waterlogged) {
			waterlogged = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
		}
		for (Direction direction : context.getNearestLookingDirections()) {
			BlockState blockState;
			if (direction.getAxis() == Direction.Axis.Y) {
				blockState = this.defaultBlockState().setValue(FACE, direction == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR).setValue(FACING, context.getHorizontalDirection()).setValue(WATERLOGGED, waterlogged);
			} else {
				blockState = this.defaultBlockState().setValue(FACE, AttachFace.WALL).setValue(FACING, direction.getOpposite()).setValue(WATERLOGGED, waterlogged);
			}
			if (blockState.canSurvive(context.getLevel(), context.getClickedPos())) {
				return blockState;
			}
		}
		return null;
	}

	@Override
	@NotNull
	public BlockState updateShape(BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState, @NotNull LevelAccessor level, @NotNull BlockPos currentPos, @NotNull BlockPos neighborPos) {
		if (state.getValue(WATERLOGGED)) {
			level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
	}

	@Override
	@NotNull
	public FluidState getFluidState(@NotNull BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	@NotNull
	public VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return switch (state.getValue(FACE)) {
			case FLOOR -> FLOOR_SHAPE;
			case WALL -> switch (state.getValue(FACING)) {
				case EAST -> EAST_WALL_SHAPE;
				case WEST -> WEST_WALL_SHAPE;
				case SOUTH -> SOUTH_WALL_SHAPE;
				default -> NORTH_WALL_SHAPE;
			};
			case CEILING -> CEILING_SHAPE;
		};
	}

	public boolean isMaxAge(@NotNull BlockState state) {
		return state.getValue(AGE) == 2;
	}

	@Override
	public void randomTick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
		if (random.nextInt(0, level.getMaxLocalRawBrightness(pos) + 2) == 1) {
			if (!isMaxAge(state)) {
				level.setBlock(pos, state.cycle(AGE), 2);
			} else if (!isFullyGrown(state)) {
				level.setBlock(pos, state.cycle(STAGE).setValue(AGE, 0), 2);
			}
		}
	}

	@Override
	public boolean isValidBonemealTarget(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state) {
		return !isFullyGrown(state);
	}

	@Override
	public boolean isBonemealSuccess(@NotNull Level level, @NotNull RandomSource random, @NotNull BlockPos pos, @NotNull BlockState state) {
		return true;
	}

	@Override
	public void performBonemeal(@NotNull ServerLevel level, @NotNull RandomSource random, @NotNull BlockPos pos, @NotNull BlockState state) {
		level.setBlock(pos, state.cycle(STAGE).setValue(AGE, 0), 2);
	}
}
