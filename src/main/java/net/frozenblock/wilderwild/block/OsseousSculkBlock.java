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

import net.frozenblock.lib.math.api.EasyNoiseSampler;
import net.frozenblock.wilderwild.registry.RegisterBlocks;
import net.frozenblock.wilderwild.registry.RegisterProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SculkBehaviour;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OsseousSculkBlock extends Block implements SculkBehaviour {
	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final IntegerProperty HEIGHT_LEFT = RegisterProperties.PILLAR_HEIGHT_LEFT;
	public static final IntegerProperty TOTAL_HEIGHT = RegisterProperties.TOTAL_HEIGHT;
	private static final ConstantInt EXPERIENCE = ConstantInt.of(3);

	public OsseousSculkBlock(@NotNull Properties settings) {
		super(settings);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP).setValue(HEIGHT_LEFT, 0).setValue(TOTAL_HEIGHT, 0));
	}

	public static Direction getDir(@NotNull Direction.Axis axis, @NotNull RandomSource random) {
		if (axis == Direction.Axis.X) {
			return random.nextBoolean() ? Direction.EAST : Direction.WEST;
		}
		return random.nextBoolean() ? Direction.NORTH : Direction.SOUTH;
	}

	@NotNull
	public static Direction.Axis getAxis(@NotNull RandomSource random) {
		return random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
	}

	@NotNull
	public static Direction.Axis getAxis(@NotNull BlockPos pos) {
		return EasyNoiseSampler.sample(EasyNoiseSampler.perlinLocal, pos, 0.7, false, false) > 0 ? Direction.Axis.X : Direction.Axis.Z;
	}

	public static boolean isSafeToReplace(@NotNull BlockState state) {
		return state.is(Blocks.SCULK_VEIN) || state.isAir() || state.is(Blocks.WATER);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(@NotNull BlockPlaceContext blockPlaceContext) {
		return super.getStateForPlacement(blockPlaceContext).setValue(FACING, blockPlaceContext.getClickedFace().getOpposite());
	}

	@Override
	public void spawnAfterBreak(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull ItemStack stack, boolean dropExperience) {
		super.spawnAfterBreak(state, level, pos, stack, dropExperience);
		if (dropExperience) {
			this.tryDropExperience(level, pos, stack, EXPERIENCE);
		}
	}

	@Override
	public int attemptUseCharge(SculkSpreader.@NotNull ChargeCursor cursor, @NotNull LevelAccessor level, @NotNull BlockPos catalystPos, @NotNull RandomSource random, @NotNull SculkSpreader spreadManager, boolean shouldConvertToBlock) {
		if (spreadManager.isWorldGeneration()) {
			worldGenSpread(cursor.getPos(), level, random);
			return cursor.getCharge();
		}
		int i = cursor.getCharge();
		int j = 1;
		if (i != 0 && random.nextInt(2) == 0) {
			BlockPos blockPos = cursor.getPos();
			boolean bl = blockPos.closerThan(catalystPos, spreadManager.noGrowthRadius());
			if (!bl) {
				int pillarHeightLeft = level.getBlockState(blockPos).getValue(OsseousSculkBlock.HEIGHT_LEFT);
				if (pillarHeightLeft > 0) {
					BlockPos topPos = getTop(level, blockPos, pillarHeightLeft);
					if (topPos != null) {
						BlockPos.MutableBlockPos mutableBlockPos = topPos.mutable();
						BlockState state = level.getBlockState(topPos);
						pillarHeightLeft = state.getValue(HEIGHT_LEFT);
						Direction direction = state.getValue(FACING);
						BlockState offsetState = level.getBlockState(mutableBlockPos.move(direction));
						if (offsetState.isAir() || offsetState.getBlock() == Blocks.SCULK_VEIN) {
							BlockState blockState = getGrowthState(random, pillarHeightLeft, state, direction);
							if (blockState.getBlock() == this) {
								blockState = blockState.setValue(TOTAL_HEIGHT, state.getValue(TOTAL_HEIGHT)).setValue(FACING, direction);
								if (direction == Direction.DOWN && random.nextDouble() > 0.8) {
									Direction nextDirection = getDir(getAxis(random), random);
									if (isSafeToReplace(level.getBlockState(mutableBlockPos.setWithOffset(topPos, nextDirection)))) {
										BlockState ribState = this.defaultBlockState().setValue(FACING, nextDirection).setValue(TOTAL_HEIGHT, state.getValue(TOTAL_HEIGHT)).setValue(HEIGHT_LEFT, 0);
										level.setBlock(mutableBlockPos, ribState, 3);
										SoundType placedSoundType = ribState.getSoundType();
										level.playSound(null, mutableBlockPos, placedSoundType.getPlaceSound(), SoundSource.BLOCKS, placedSoundType.getVolume(), placedSoundType.getPitch());
										if (isSafeToReplace(level.getBlockState(mutableBlockPos.move(Direction.DOWN))) && random.nextDouble() > 0.7) {
											BlockState tendrilState = RegisterBlocks.HANGING_TENDRIL.defaultBlockState();
											level.setBlock(mutableBlockPos, tendrilState, 3);
											SoundType tendrilSoundType = tendrilState.getSoundType();
											level.playSound(null, mutableBlockPos, tendrilSoundType.getPlaceSound(), SoundSource.BLOCKS, tendrilSoundType.getVolume(), tendrilSoundType.getPitch());
										}
									}
								}
							}
							level.setBlock(mutableBlockPos.setWithOffset(topPos, direction), blockState, 3);
							SoundType placedSoundType = blockState.getSoundType();
							level.playSound(null, mutableBlockPos, placedSoundType.getPlaceSound(), SoundSource.BLOCKS, placedSoundType.getVolume(), placedSoundType.getPitch());
							workOnBottom(level, mutableBlockPos, state);
							return Math.max(0, i - j);
						}
					}
				}
			}
		}
		return i;
	}

	public void worldGenSpread(@NotNull BlockPos blockPos, @NotNull LevelAccessor level, @NotNull RandomSource random) {
		BlockState firstState = level.getBlockState(blockPos);
		if (firstState.is(this)) {
			int pillarHeightLeft = firstState.getValue(HEIGHT_LEFT);
			if (pillarHeightLeft > 0) {
				BlockPos topPos = getTop(level, blockPos, pillarHeightLeft);
				if (topPos != null) {
					BlockPos.MutableBlockPos mutableBlockPos = topPos.mutable();
					BlockState state = level.getBlockState(topPos);
					pillarHeightLeft = state.getValue(HEIGHT_LEFT);
					Direction direction = state.getValue(FACING);
					BlockState offsetState = level.getBlockState(mutableBlockPos.move(direction));
					if (offsetState.isAir() || offsetState.getBlock() == Blocks.SCULK_VEIN) {
						BlockState blockState = getGrowthState(random, pillarHeightLeft, state, direction);
						if (blockState.getBlock() == this) {
							blockState = blockState.setValue(TOTAL_HEIGHT, state.getValue(TOTAL_HEIGHT)).setValue(FACING, direction);
							if (direction == Direction.DOWN && random.nextDouble() > 0.8) {
								Direction nextDirection = getDir(getAxis(random), random);
								if (isSafeToReplace(level.getBlockState(mutableBlockPos.setWithOffset(topPos, nextDirection)))) {
									level.setBlock(mutableBlockPos, this.defaultBlockState().setValue(FACING, nextDirection).setValue(TOTAL_HEIGHT, state.getValue(TOTAL_HEIGHT)).setValue(HEIGHT_LEFT, 0), 3);
									if (isSafeToReplace(level.getBlockState(mutableBlockPos.move(Direction.DOWN)))) {
										if (random.nextDouble() > 0.6) {
											level.setBlock(mutableBlockPos, RegisterBlocks.HANGING_TENDRIL.defaultBlockState(), 3);
										}
									}
								}
							}
						}
						level.setBlock(mutableBlockPos.setWithOffset(topPos, direction), blockState, 3);
						workOnBottom(level, topPos, state);
					}
				}
			}
		}
	}

	private BlockState getGrowthState(@NotNull RandomSource random, int pillarHeightLeft, @NotNull BlockState state, @NotNull Direction direction) {
		BlockState blockState = this.defaultBlockState().setValue(HEIGHT_LEFT, Math.max(0, pillarHeightLeft - 1));
		if (
			pillarHeightLeft == 1 && direction == Direction.UP && state.getValue(TOTAL_HEIGHT) > 0
				&& EasyNoiseSampler.localRandom.nextInt(Math.max(1, state.getValue(TOTAL_HEIGHT) / 2)) <= 1
				&& random.nextInt(11) == 0
		) {
			blockState = Blocks.SCULK_CATALYST.defaultBlockState();
		}
		return blockState;
	}

	private void workOnBottom(@NotNull LevelAccessor level, @NotNull BlockPos topPos, @NotNull BlockState state) {
		BlockPos bottom = getBottom(level, topPos, state.getValue(TOTAL_HEIGHT));
		if (bottom != null) {
			BlockState bottomState = level.getBlockState(bottom);
			if (bottomState.is(this)) {
				int total = bottomState.getValue(TOTAL_HEIGHT);
				if ((total) - bottomState.getValue(HEIGHT_LEFT) <= total / 3) {
					this.convertToSculk(level, bottom);
				}
			}
		}
	}

	public void convertToSculk(@NotNull LevelAccessor level, @NotNull BlockPos pos) {
		BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();
		BlockState state = level.getBlockState(mutableBlockPos);
		if (state.is(this)) {
			BlockState stateReplace;
			Direction oppositeDirection;
			for (Direction direction : UPDATE_SHAPE_ORDER) {
				stateReplace = level.getBlockState(mutableBlockPos.move(direction));
				oppositeDirection = direction.getOpposite();
				if (stateReplace.is(Blocks.SCULK_VEIN)) {
					stateReplace = stateReplace.setValue(MultifaceBlock.getFaceProperty(oppositeDirection), false);
					if (MultifaceBlock.availableFaces(stateReplace).isEmpty()) {
						stateReplace = stateReplace.getValue(BlockStateProperties.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
					}
					level.setBlock(mutableBlockPos, stateReplace, 3);
				}
				mutableBlockPos.move(oppositeDirection);
			}
			mutableBlockPos.move(state.getValue(FACING));
			for (Direction direction : UPDATE_SHAPE_ORDER) {
				stateReplace = level.getBlockState(mutableBlockPos.move(direction));
				oppositeDirection = direction.getOpposite();
				BlockState stateSetTo = null;
				if (stateReplace.is(Blocks.SCULK_VEIN)) {
					stateSetTo = stateReplace.setValue(MultifaceBlock.getFaceProperty(oppositeDirection), true);
				}
				if (stateReplace.isAir() && stateReplace.getFluidState().isEmpty()) {
					stateSetTo = Blocks.SCULK_VEIN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(oppositeDirection), true);
				}
				if (stateReplace.getBlock() == Blocks.WATER) {
					stateSetTo = Blocks.SCULK_VEIN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(oppositeDirection), true).setValue(BlockStateProperties.WATERLOGGED, true);
				}
				if (stateSetTo != null) {
					level.setBlock(mutableBlockPos, stateSetTo, 3);
				}
				mutableBlockPos.move(oppositeDirection);
			}
			level.setBlock(pos, Blocks.SCULK.defaultBlockState(), 3);
		}
	}

	@Nullable
	public BlockPos getTop(@NotNull LevelAccessor level, @NotNull BlockPos pos, int max) {
		BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();
		BlockPos.MutableBlockPos mutableBlockPos2 = pos.mutable();
		for (int i = 0; i < max; i++) {
			BlockState blockState = level.getBlockState(mutableBlockPos);
			if (blockState.getBlock() != this) {
				return null;
			}
			BlockState offsetState = level.getBlockState(mutableBlockPos2.move(blockState.getValue(FACING)));
			if (offsetState.isAir() || offsetState.getBlock() == Blocks.SCULK_VEIN) {
				return mutableBlockPos.immutable();
			}
			mutableBlockPos.set(mutableBlockPos2);
		}
		return null;
	}

	@Nullable
	public BlockPos getBottom(@NotNull LevelAccessor level, @NotNull BlockPos pos, int max) {
		BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();
		BlockPos.MutableBlockPos mutableBlockPos2 = pos.mutable();
		for (int i = 0; i < max; i++) {
			BlockState blockState = level.getBlockState(mutableBlockPos);
			if (blockState.getBlock() != this) {
				return null;
			}
			if (level.getBlockState(mutableBlockPos2.move(blockState.getValue(FACING), -1)).is(Blocks.SCULK)) {
				return mutableBlockPos.immutable();
			}
			mutableBlockPos.set(mutableBlockPos2);
		}
		return null;
	}

	@Override
	public BlockState rotate(@NotNull BlockState blockState, @NotNull Rotation rotation) {
		return blockState.setValue(FACING, rotation.rotate(blockState.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING).add(HEIGHT_LEFT).add(TOTAL_HEIGHT);
	}
}
