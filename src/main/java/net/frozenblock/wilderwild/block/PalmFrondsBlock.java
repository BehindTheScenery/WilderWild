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

import net.frozenblock.wilderwild.block.entity.PalmCrownBlockEntity;
import net.frozenblock.wilderwild.registry.RegisterBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

public class PalmFrondsBlock extends LeavesBlock implements BonemealableBlock {

	public PalmFrondsBlock(@NotNull Properties settings) {
		super(settings);
	}

	public static boolean nextToLeafOrCrown(@NotNull BlockState neighbor) {
		return neighbor.is(RegisterBlocks.PALM_FRONDS) || neighbor.is(RegisterBlocks.PALM_CROWN);
	}

	@NotNull
	public static BlockState updateDistance(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
		int dist = Mth.clamp((int) (PalmCrownBlockEntity.PalmCrownPositions.distanceToClosestPalmCrown(level, pos, 7)), 1, 7);
		int i = 7;
		boolean validCrown = false;
		for (BlockPos blockPos : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
			if (!validCrown && nextToLeafOrCrown(level.getBlockState(blockPos))) {
				validCrown = true;
			}
			i = Math.min(i, getDistanceAt(level.getBlockState(blockPos)) + 1);
			if (i == 1) break;
		}
		return state.setValue(DISTANCE, Math.min(validCrown ? dist : i, i));
	}

	public static int getDistanceAt(@NotNull BlockState neighbor) {
		if (neighbor.is(BlockTags.LOGS)) {
			return 0;
		}
		return 7;
	}

	@Override
	public boolean isValidBonemealTarget(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state, boolean isClient) {
		return level.getBlockState(pos.below()).isAir() && (state.getValue(BlockStateProperties.DISTANCE) < 2 || state.getValue(BlockStateProperties.PERSISTENT));
	}

	@Override
	public boolean isBonemealSuccess(@NotNull Level level, @NotNull RandomSource random, @NotNull BlockPos pos, @NotNull BlockState state) {
		return true;
	}

	@Override
	public void performBonemeal(@NotNull ServerLevel level, @NotNull RandomSource random, @NotNull BlockPos pos, @NotNull BlockState state) {
		level.setBlock(pos.below(), CoconutBlock.getDefaultHangingState(), 2);
	}

	@Override
	public void randomTick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
		BlockState newState = updateDistance(state, level, pos);
		if (newState != state) {
			level.setBlockAndUpdate(pos, newState);
			state = newState;
		}
		if (this.decaying(state)) {
			LeavesBlock.dropResources(state, level, pos);
			level.removeBlock(pos, false);
		}
	}

	@Override
	public void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
		level.setBlock(pos, updateDistance(state, level, pos), 3);
	}

	@Override
	@NotNull
	public BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState, @NotNull LevelAccessor level, @NotNull BlockPos currentPos, @NotNull BlockPos neighborPos) {
		int i;
		if (state.getValue(WATERLOGGED)) {
			level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		if ((i = getDistanceAt(neighborState) + 1) != 1 || state.getValue(DISTANCE) != i) {
			level.scheduleTick(currentPos, this, 1);
		}
		return state;
	}

	@Override
	public boolean isRandomlyTicking(@NotNull BlockState state) {
		return !state.getValue(PERSISTENT);
	}
}
