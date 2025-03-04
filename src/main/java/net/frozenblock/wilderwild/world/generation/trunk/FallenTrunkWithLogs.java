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

package net.frozenblock.wilderwild.world.generation.trunk;

import com.google.common.collect.Lists;
import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import net.frozenblock.wilderwild.registry.RegisterFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import org.jetbrains.annotations.NotNull;

public class FallenTrunkWithLogs extends TrunkPlacer {
	public static final Codec<FallenTrunkWithLogs> CODEC = RecordCodecBuilder.create((instance) ->
		fallenTrunkCodec(instance).apply(instance, FallenTrunkWithLogs::new));

	public final BlockStateProvider hollowedState;
	public final float logChance;
	public final IntProvider maxLogs;
	public final float hollowedChance;
	public final float successInWaterChance;

	public FallenTrunkWithLogs(int baseHeight, int firstRandomHeight, int secondRandomHeight, BlockStateProvider hollowedState, float logChance, float successInWaterChance, float hollowedChance, @NotNull IntProvider maxLogs) {
		super(baseHeight, firstRandomHeight, secondRandomHeight);
		this.hollowedState = hollowedState;
		this.logChance = logChance;
		this.maxLogs = maxLogs;
		this.hollowedChance = hollowedChance;
		this.successInWaterChance = successInWaterChance;
	}

	protected static <P extends FallenTrunkWithLogs> Products.P8<RecordCodecBuilder.Mu<P>, Integer, Integer, Integer, BlockStateProvider, Float, Float, Float, IntProvider> fallenTrunkCodec(RecordCodecBuilder.Instance<P> builder) {
		return trunkPlacerParts(builder)
			.and(BlockStateProvider.CODEC.fieldOf("hollowed_state").forGetter((trunkPlacer) -> trunkPlacer.hollowedState))
			.and(Codec.floatRange(0.0F, 1.0F).fieldOf("place_branch_chance").forGetter((trunkPlacer) -> trunkPlacer.logChance))
			.and(Codec.floatRange(0.0F, 1.0F).fieldOf("success_in_water_chance").forGetter((trunkPlacer) -> trunkPlacer.successInWaterChance))
			.and(Codec.floatRange(0.0F, 1.0F).fieldOf("hollow_chance").forGetter((trunkPlacer) -> trunkPlacer.hollowedChance))
			.and(IntProvider.NON_NEGATIVE_CODEC.fieldOf("max_logs").forGetter((trunkPlacer) -> trunkPlacer.maxLogs));
	}

	private static boolean isWaterAt(@NotNull LevelSimulatedReader level, @NotNull BlockPos blockpos) {
		return level.isFluidAtPosition(blockpos, fluidState -> fluidState.is(FluidTags.WATER));
	}

	@Override
	@NotNull
	protected TrunkPlacerType<?> type() {
		return RegisterFeatures.FALLEN_TRUNK_WITH_LOGS_PLACER_TYPE;
	}

	@Override
	@NotNull
	public List<FoliagePlacer.FoliageAttachment> placeTrunk(@NotNull LevelSimulatedReader level, @NotNull BiConsumer<BlockPos, BlockState> replacer, @NotNull RandomSource random, int height, @NotNull BlockPos startPos, @NotNull TreeConfiguration config) {
		List<FoliagePlacer.FoliageAttachment> list = Lists.newArrayList();
		List<BlockPos> logs = Lists.newArrayList();
		BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
		int maxLogs = this.maxLogs.sample(random);
		boolean hollow = random.nextFloat() < this.hollowedChance;
		Direction logDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		int extraLogs = 0;
		if (isWaterAt(level, startPos) && random.nextFloat() > this.successInWaterChance) {
			return list;
		}

		BlockPos endPos = startPos.relative(logDir, height);
		BlockPos secondToEndPos = endPos.relative(logDir.getOpposite());
		int aboveSolidAmount = 0;
		boolean isEndAboveSolid = false;
		Iterable<BlockPos> poses = BlockPos.betweenClosed(startPos, endPos);
		for (BlockPos blockPos : poses) {
			mutable.set(blockPos);
			if (TreeFeature.validTreePos(level, mutable)) {
				if (!TreeFeature.validTreePos(level, mutable.move(Direction.DOWN)) && !TreeFeature.isAirOrLeaves(level, mutable)) {
					aboveSolidAmount += 1;
					mutable.move(Direction.UP);
					if (mutable.equals(endPos) || mutable.equals(secondToEndPos)) {
						isEndAboveSolid = true;
					}
				} else {
					mutable.move(Direction.UP);
					if (mutable.equals(startPos)) {
						return list;
					}
				}
			} else {
				return list;
			}
		}

		if (isEndAboveSolid || ((double) aboveSolidAmount / (double) height) > 0.5) {
			for (BlockPos blockPos : poses) {
				mutable.set(blockPos);
				placeLog(logs, level, replacer, random, config, mutable, logDir, hollow);
				if (random.nextFloat() < this.logChance && extraLogs < maxLogs) {
					Direction direction = random.nextFloat() >= 0.33 ? Direction.Plane.HORIZONTAL.getRandomDirection(random) : Direction.Plane.VERTICAL.getRandomDirection(random);
					this.generateExtraBranch(logs, level, replacer, random, config, mutable, logDir, direction, hollow);
					++extraLogs;
				}
			}
		}

		return list;
	}

	private void generateExtraBranch(@NotNull List<BlockPos> logs, LevelSimulatedReader level, @NotNull BiConsumer<BlockPos, BlockState> replacer, @NotNull RandomSource random, @NotNull TreeConfiguration config, @NotNull BlockPos.MutableBlockPos pos, @NotNull Direction offsetDir, @NotNull Direction direction, boolean hollow) {
		int x = pos.getX();
		int z = pos.getZ();
		int y = pos.getY();
		if (offsetDir.getAxis() != direction.getAxis()) {
			x += direction.getStepX();
			z += direction.getStepZ();
			y += direction.getStepY();
			if (TreeFeature.validTreePos(level, pos.set(x, y, z))) {
				placeLog(logs, level, replacer, random, config, pos, direction, hollow);
			}
		}
	}

	private void placeLog(@NotNull List<BlockPos> logs, LevelSimulatedReader level, @NotNull BiConsumer<BlockPos, BlockState> replacer, @NotNull RandomSource random, @NotNull TreeConfiguration config, @NotNull BlockPos.MutableBlockPos pos, @NotNull Direction direction, boolean hollow) {
		BlockState setState = !hollow ? config.trunkProvider.getState(random, pos) : this.hollowedState.getState(random, pos);
		if (setState.hasProperty(BlockStateProperties.AXIS)) {
			Direction.Axis axis = direction.getStepX() != 0 ? Direction.Axis.X : (direction.getStepY() != 0 ? Direction.Axis.Y : Direction.Axis.Z);
			setState = setState.setValue(BlockStateProperties.AXIS, axis);
		}
		if (setState.hasProperty(BlockStateProperties.WATERLOGGED)) {
			setState = setState.setValue(BlockStateProperties.WATERLOGGED, isWaterAt(level, pos));
		}
		replacer.accept(pos, setState);
		logs.add(pos.immutable());
	}

}
