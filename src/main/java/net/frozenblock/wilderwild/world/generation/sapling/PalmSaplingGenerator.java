package net.frozenblock.wilderwild.world.generation.sapling;

import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.annotation.Nullable;
import net.frozenblock.wilderwild.world.additions.feature.WilderTreeConfigured;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class PalmSaplingGenerator extends AbstractTreeGrower {

	public PalmSaplingGenerator() {

	}

	@Nullable
	protected Holder<? extends ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean largeHive) {
		return random.nextDouble() > 0.4 ? WilderTreeConfigured.PALM : WilderTreeConfigured.TALL_PALM;
	}
}
