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

package net.frozenblock.wilderwild.misc;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBiomeTags;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBlockTags;
import net.frozenblock.lib.datagen.api.FrozenBiomeTagProvider;
import net.frozenblock.lib.feature_flag.api.FrozenFeatureFlags;
import net.frozenblock.lib.tag.api.FrozenBiomeTags;
import net.frozenblock.lib.tag.api.FrozenItemTags;
import net.frozenblock.wilderwild.registry.RegisterBlocks;
import net.frozenblock.wilderwild.registry.RegisterDamageTypes;
import net.frozenblock.wilderwild.registry.RegisterEntities;
import net.frozenblock.wilderwild.registry.RegisterItems;
import net.frozenblock.wilderwild.registry.RegisterStructures;
import net.frozenblock.wilderwild.registry.RegisterWorldgen;
import net.frozenblock.wilderwild.tag.WilderBiomeTags;
import net.frozenblock.wilderwild.tag.WilderBlockTags;
import net.frozenblock.wilderwild.tag.WilderEntityTags;
import net.frozenblock.wilderwild.tag.WilderItemTags;
import net.frozenblock.wilderwild.world.generation.WilderFeatureBootstrap;
import net.frozenblock.wilderwild.world.generation.noise.WilderNoise;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

public class WilderWildDataGenerator implements DataGeneratorEntrypoint {

	public static <T> HolderLookup.RegistryLookup<T> asLookup(HolderGetter<T> getter) {
		return (HolderLookup.RegistryLookup<T>) getter;
	}

	@Override
	public void onInitializeDataGenerator(@NotNull FabricDataGenerator dataGenerator) {
		WilderFeatureFlags.init();
		FrozenFeatureFlags.rebuild();
		final FabricDataGenerator.Pack pack = dataGenerator.createPack();
		pack.addProvider(WilderBlockLootProvider::new);
		pack.addProvider(WilderRegistryProvider::new);
		pack.addProvider(WilderBiomeTagProvider::new);
		pack.addProvider(WilderBlockTagProvider::new);
		pack.addProvider(WilderDamageTypeTagProvider::new);
		pack.addProvider(WilderItemTagProvider::new);
		pack.addProvider(WilderEntityTagProvider::new);
	}

	@Override
	public void buildRegistry(@NotNull RegistrySetBuilder registryBuilder) {
		WilderSharedConstants.logWild("Registering Biomes for", WilderSharedConstants.UNSTABLE_LOGGING);

		registryBuilder.add(Registries.DAMAGE_TYPE, RegisterDamageTypes::bootstrap);
		registryBuilder.add(Registries.CONFIGURED_FEATURE, WilderFeatureBootstrap::bootstrapConfigured);
		registryBuilder.add(Registries.PLACED_FEATURE, WilderFeatureBootstrap::bootstrapPlaced);
		registryBuilder.add(Registries.BIOME, RegisterWorldgen::bootstrap);
		registryBuilder.add(Registries.NOISE, WilderNoise::bootstrap);
		registryBuilder.add(Registries.PROCESSOR_LIST, RegisterStructures::bootstrapProcessor);
		registryBuilder.add(Registries.TEMPLATE_POOL, RegisterStructures::bootstrapTemplatePool);
		registryBuilder.add(Registries.STRUCTURE, RegisterStructures::bootstrap);
		registryBuilder.add(Registries.STRUCTURE_SET, RegisterStructures::bootstrapStructureSet);
	}

	private static class WilderRegistryProvider extends FabricDynamicRegistryProvider {

		public WilderRegistryProvider(@NotNull FabricDataOutput output, @NotNull CompletableFuture<HolderLookup.Provider> registriesFuture) {
			super(output, registriesFuture);
		}

		@Override
		protected void configure(@NotNull HolderLookup.Provider registries, @NotNull Entries entries) {
			final var damageTypes = asLookup(entries.getLookup(Registries.DAMAGE_TYPE));

			entries.addAll(damageTypes);

			WilderFeatureBootstrap.bootstrap(entries);
		}

		@Override
		@NotNull
		public String getName() {
			return "Wilder Wild Dynamic Registries";
		}
	}

	private static class WilderBiomeTagProvider extends FrozenBiomeTagProvider {

		public WilderBiomeTagProvider(@NotNull FabricDataOutput output, @NotNull CompletableFuture registriesFuture) {
			super(output, registriesFuture);
		}

		@Override
		protected void addTags(@NotNull HolderLookup.Provider arg) {
			this.generateBiomeTags();
			this.generateClimateAndVegetationTags();
			this.generateUtilityTags();
			this.generateFeatureTags();
			this.generateStructureTags();
		}

		private void generateBiomeTags() {
			this.getOrCreateTagBuilder(WilderBiomeTags.WILDER_WILD_BIOMES)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.OASIS)
				.addOptional(RegisterWorldgen.WARM_RIVER)
				.addOptional(RegisterWorldgen.WARM_BEACH)
				.addOptional(RegisterWorldgen.JELLYFISH_CAVES)
				.addOptional(RegisterWorldgen.ARID_FOREST)
				.addOptional(RegisterWorldgen.ARID_SAVANNA)
				.addOptional(RegisterWorldgen.PARCHED_FOREST)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.FLOWER_FIELD)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST)
				.addOptional(RegisterWorldgen.SNOWY_OLD_GROWTH_PINE_TAIGA)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(BiomeTags.IS_OVERWORLD)
				.addOptionalTag(WilderBiomeTags.WILDER_WILD_BIOMES);

			this.getOrCreateTagBuilder(BiomeTags.IS_JUNGLE)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE);

			this.getOrCreateTagBuilder(ConventionalBiomeTags.CAVES)
				.addOptional(RegisterWorldgen.JELLYFISH_CAVES);

			this.getOrCreateTagBuilder(WilderBiomeTags.DARK_FOREST)
				.add(Biomes.DARK_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.GROVE)
				.add(Biomes.GROVE);

			this.getOrCreateTagBuilder(WilderBiomeTags.MEADOW)
				.add(Biomes.MEADOW);

			this.getOrCreateTagBuilder(WilderBiomeTags.OAK_SAPLINGS_GROW_SWAMP_VARIANT)
				.add(Biomes.SWAMP)
				.add(Biomes.MANGROVE_SWAMP)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS)
				.addOptionalTag(BiomeTags.IS_OCEAN);

			this.getOrCreateTagBuilder(WilderBiomeTags.NON_FROZEN_PLAINS)
				.add(Biomes.PLAINS)
				.add(Biomes.SUNFLOWER_PLAINS)
				.addOptional(RegisterWorldgen.FLOWER_FIELD);

			this.getOrCreateTagBuilder(WilderBiomeTags.NORMAL_SAVANNA)
				.add(Biomes.SAVANNA)
				.add(Biomes.SAVANNA_PLATEAU)
				.addOptional(RegisterWorldgen.ARID_SAVANNA);

			this.getOrCreateTagBuilder(WilderBiomeTags.SHORT_TAIGA)
				.add(Biomes.TAIGA)
				.add(Biomes.SNOWY_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.SNOWY_PLAINS)
				.add(Biomes.SNOWY_PLAINS);

			this.getOrCreateTagBuilder(WilderBiomeTags.TALL_PINE_TAIGA)
				.add(Biomes.OLD_GROWTH_PINE_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.TALL_SPRUCE_TAIGA)
				.add(Biomes.OLD_GROWTH_SPRUCE_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.WINDSWEPT_FOREST)
				.add(Biomes.WINDSWEPT_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.WINDSWEPT_HILLS)
				.add(Biomes.WINDSWEPT_HILLS)
				.add(Biomes.WINDSWEPT_GRAVELLY_HILLS);

			this.getOrCreateTagBuilder(WilderBiomeTags.WINDSWEPT_SAVANNA)
				.add(Biomes.WINDSWEPT_SAVANNA);

			this.getOrCreateTagBuilder(BiomeTags.IS_TAIGA)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(BiomeTags.IS_SAVANNA)
				.addOptional(RegisterWorldgen.ARID_SAVANNA);

			this.getOrCreateTagBuilder(BiomeTags.IS_RIVER)
				.addOptional(RegisterWorldgen.WARM_RIVER)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS);

			this.getOrCreateTagBuilder(BiomeTags.IS_BEACH)
				.addOptional(RegisterWorldgen.WARM_BEACH);

			this.getOrCreateTagBuilder(ConventionalBiomeTags.BEACH)
				.addOptional(RegisterWorldgen.WARM_BEACH);

			this.getOrCreateTagBuilder(WilderBiomeTags.RAINFOREST)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST);
		}

		private void generateClimateAndVegetationTags() {
			this.getOrCreateTagBuilder(ConventionalBiomeTags.CLIMATE_HOT)
				.addOptional(RegisterWorldgen.OASIS)
				.addOptional(RegisterWorldgen.ARID_FOREST)
				.addOptional(RegisterWorldgen.ARID_SAVANNA);

			this.getOrCreateTagBuilder(ConventionalBiomeTags.CLIMATE_TEMPERATE)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.PARCHED_FOREST)
				.addOptional(RegisterWorldgen.WARM_RIVER)
				.addOptional(RegisterWorldgen.WARM_BEACH)
				.addOptional(RegisterWorldgen.FLOWER_FIELD)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(ConventionalBiomeTags.CLIMATE_COLD)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.SNOWY_OLD_GROWTH_PINE_TAIGA);

			this.getOrCreateTagBuilder(ConventionalBiomeTags.CLIMATE_WET)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS)
				.addOptional(RegisterWorldgen.OASIS)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(ConventionalBiomeTags.CLIMATE_DRY)
				.addOptional(RegisterWorldgen.ARID_SAVANNA)
				.addOptional(RegisterWorldgen.ARID_FOREST)
				.addOptional(RegisterWorldgen.PARCHED_FOREST);

			this.getOrCreateTagBuilder(ConventionalBiomeTags.TREE_CONIFEROUS)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.SNOWY_OLD_GROWTH_PINE_TAIGA)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(ConventionalBiomeTags.TREE_DECIDUOUS)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(ConventionalBiomeTags.VEGETATION_DENSE)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.RAINFOREST);

			this.getOrCreateTagBuilder(ConventionalBiomeTags.FLORAL)
				.add(Biomes.SUNFLOWER_PLAINS)
				.addOptional(RegisterWorldgen.FLOWER_FIELD)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS);

			this.getOrCreateTagBuilder(ConventionalBiomeTags.SNOWY)
				.addOptional(RegisterWorldgen.SNOWY_OLD_GROWTH_PINE_TAIGA);

			this.getOrCreateTagBuilder(ConventionalBiomeTags.AQUATIC)
				.addOptional(RegisterWorldgen.WARM_RIVER)
				.addOptional(RegisterWorldgen.JELLYFISH_CAVES);

			this.getOrCreateTagBuilder(ConventionalBiomeTags.TREE_JUNGLE)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE);

			this.getOrCreateTagBuilder(ConventionalBiomeTags.JUNGLE)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE);

			this.getOrCreateTagBuilder(ConventionalBiomeTags.TREE_SAVANNA)
				.addOptional(RegisterWorldgen.PARCHED_FOREST)
				.addOptional(RegisterWorldgen.ARID_SAVANNA);

			this.getOrCreateTagBuilder(ConventionalBiomeTags.SAVANNA)
				.addOptional(RegisterWorldgen.ARID_SAVANNA);

			this.getOrCreateTagBuilder(ConventionalBiomeTags.DESERT)
				.addOptional(RegisterWorldgen.OASIS);

			this.getOrCreateTagBuilder(ConventionalBiomeTags.FOREST)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.PARCHED_FOREST)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.ARID_FOREST)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA);

			this.getOrCreateTagBuilder(ConventionalBiomeTags.RIVER)
				.addOptional(RegisterWorldgen.WARM_RIVER)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS);

			this.getOrCreateTagBuilder(ConventionalBiomeTags.SWAMP)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS);

			this.getOrCreateTagBuilder(ConventionalBiomeTags.UNDERGROUND)
				.addOptional(RegisterWorldgen.JELLYFISH_CAVES);

			this.getOrCreateTagBuilder(WilderBiomeTags.LUKEWARM_WATER)
				.add(Biomes.DARK_FOREST)
				.add(Biomes.SAVANNA)
				.add(Biomes.SAVANNA_PLATEAU)
				.add(Biomes.WINDSWEPT_SAVANNA)
				.add(Biomes.JUNGLE)
				.add(Biomes.BAMBOO_JUNGLE)
				.add(Biomes.SPARSE_JUNGLE)
				.addOptional(RegisterWorldgen.DARK_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST)
				.addOptional(RegisterWorldgen.PARCHED_FOREST)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.WARM_RIVER)
				.addOptional(RegisterWorldgen.WARM_BEACH);

			this.getOrCreateTagBuilder(WilderBiomeTags.HOT_WATER)
				.add(Biomes.DESERT)
				.add(Biomes.BADLANDS)
				.add(Biomes.ERODED_BADLANDS)
				.add(Biomes.WOODED_BADLANDS)
				.addOptional(RegisterWorldgen.ARID_FOREST)
				.addOptional(RegisterWorldgen.ARID_SAVANNA);

			this.getOrCreateTagBuilder(WilderBiomeTags.SNOWY_WATER)
				.add(Biomes.SNOWY_TAIGA)
				.add(Biomes.SNOWY_BEACH)
				.add(Biomes.SNOWY_PLAINS)
				.add(Biomes.SNOWY_SLOPES)
				.add(Biomes.GROVE)
				.addOptional(RegisterWorldgen.SNOWY_OLD_GROWTH_PINE_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.FROZEN_WATER)
				.add(Biomes.FROZEN_RIVER)
				.add(Biomes.FROZEN_OCEAN)
				.add(Biomes.FROZEN_PEAKS)
				.add(Biomes.ICE_SPIKES)
				.add(Biomes.FROZEN_PEAKS);
		}

		private void generateUtilityTags() {
			this.getOrCreateTagBuilder(WilderBiomeTags.FIREFLY_SPAWNABLE)
				.add(Biomes.JUNGLE)
				.add(Biomes.SPARSE_JUNGLE)
				.add(Biomes.BAMBOO_JUNGLE)
				.add(Biomes.DARK_FOREST)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.FIREFLY_SPAWNABLE_CAVE);

			this.getOrCreateTagBuilder(WilderBiomeTags.FIREFLY_SPAWNABLE_DURING_DAY)
				.add(Biomes.SWAMP)
				.add(Biomes.MANGROVE_SWAMP);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_JELLYFISH)
				.add(Biomes.WARM_OCEAN)
				.add(Biomes.DEEP_LUKEWARM_OCEAN)
				.add(Biomes.LUKEWARM_OCEAN)
				.addOptional(RegisterWorldgen.JELLYFISH_CAVES);

			this.getOrCreateTagBuilder(WilderBiomeTags.NO_POOLS)
				.addOptional(Biomes.DEEP_DARK);

			this.getOrCreateTagBuilder(WilderBiomeTags.PEARLESCENT_JELLYFISH)
				.addOptional(RegisterWorldgen.JELLYFISH_CAVES);

			this.getOrCreateTagBuilder(WilderBiomeTags.JELLYFISH_SPECIAL_SPAWN)
				.addOptional(RegisterWorldgen.JELLYFISH_CAVES);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_TUMBLEWEED_ENTITY)
				.add(Biomes.DESERT)
				.add(Biomes.WINDSWEPT_SAVANNA)
				.addOptionalTag(BiomeTags.IS_BADLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_CLAY_PATH)
				.addOptionalTag(WilderBiomeTags.SAND_BEACHES)
				.addOptionalTag(WilderBiomeTags.MULTI_LAYER_SAND_BEACHES)
				.addOptional(RegisterWorldgen.OASIS)
				.addOptional(RegisterWorldgen.WARM_RIVER)
				.addOptional(RegisterWorldgen.WARM_BEACH);

			this.getOrCreateTagBuilder(FrozenBiomeTags.CAN_LIGHTNING_OVERRIDE)
				.add(Biomes.DESERT)
				.add(Biomes.BADLANDS)
				.add(Biomes.ERODED_BADLANDS);

			this.getOrCreateTagBuilder(BiomeTags.HAS_CLOSER_WATER_FOG)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS)
				.addOptional(RegisterWorldgen.JELLYFISH_CAVES);

			this.getOrCreateTagBuilder(BiomeTags.IS_FOREST)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.PARCHED_FOREST)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.ARID_FOREST)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA);

			this.getOrCreateTagBuilder(BiomeTags.MORE_FREQUENT_DROWNED_SPAWNS)
				.addOptional(RegisterWorldgen.WARM_RIVER)
				.addOptional(RegisterWorldgen.WARM_BEACH)
				.addOptional(RegisterWorldgen.JELLYFISH_CAVES)
				.addOptional(RegisterWorldgen.OASIS)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS);

			this.getOrCreateTagBuilder(BiomeTags.SPAWNS_GOLD_RABBITS)
				.addOptional(RegisterWorldgen.OASIS)
				.addOptional(RegisterWorldgen.ARID_FOREST)
				.addOptional(RegisterWorldgen.ARID_SAVANNA);

			this.getOrCreateTagBuilder(BiomeTags.STRONGHOLD_BIASED_TO)
				.addOptionalTag(WilderBiomeTags.WILDER_WILD_BIOMES);

			this.getOrCreateTagBuilder(BiomeTags.WATER_ON_MAP_OUTLINES)
				.addOptional(RegisterWorldgen.WARM_RIVER)
				.addOptional(RegisterWorldgen.JELLYFISH_CAVES)
				.addOptional(RegisterWorldgen.OASIS)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.GRAVEL_BEACH)
				.add(Biomes.BIRCH_FOREST)
				.add(Biomes.FROZEN_RIVER)
				.add(Biomes.OLD_GROWTH_BIRCH_FOREST)
				.add(Biomes.OLD_GROWTH_PINE_TAIGA)
				.add(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
				.add(Biomes.TAIGA)
				.add(Biomes.SNOWY_TAIGA)
				.add(Biomes.RIVER)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.SAND_BEACHES)
				.add(Biomes.DARK_FOREST)
				.add(Biomes.FLOWER_FOREST)
				.add(Biomes.FOREST)
				.add(Biomes.FROZEN_RIVER)
				.add(Biomes.RIVER)
				.addOptional(RegisterWorldgen.PARCHED_FOREST)
				.addOptional(RegisterWorldgen.ARID_FOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.MULTI_LAYER_SAND_BEACHES)
				.add(Biomes.BAMBOO_JUNGLE)
				.add(Biomes.JUNGLE)
				.add(Biomes.SAVANNA)
				.add(Biomes.SPARSE_JUNGLE)
				.add(Biomes.CHERRY_GROVE)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.ARID_SAVANNA)
				.addOptional(new ResourceLocation("terralith", "arid_highlands"));
		}

		private void generateFeatureTags() {
			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_FALLEN_BIRCH_TREES)
				.add(Biomes.BIRCH_FOREST)
				.add(Biomes.OLD_GROWTH_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_FALLEN_CHERRY_TREES)
				.addOptional(Biomes.CHERRY_GROVE);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_FALLEN_OAK_AND_BIRCH_TREES)
				.add(Biomes.FOREST)
				.add(Biomes.FLOWER_FOREST)
				.addOptional(RegisterWorldgen.PARCHED_FOREST)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_FALLEN_OAK_AND_SPRUCE_TREES)
				.add(Biomes.WINDSWEPT_FOREST)
				.add(Biomes.WINDSWEPT_HILLS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_FALLEN_OAK_AND_CYPRESS_TREES)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_MOSSY_FALLEN_MIXED_TREES)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_MOSSY_FALLEN_OAK_AND_BIRCH)
				.addOptional(RegisterWorldgen.RAINFOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_FALLEN_ACACIA_AND_OAK)
				.addOptionalTag(BiomeTags.IS_SAVANNA)
				.addOptionalTag(ConventionalBiomeTags.SAVANNA)
				.addOptionalTag(ConventionalBiomeTags.TREE_SAVANNA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_FALLEN_PALM)
				.addOptional(RegisterWorldgen.OASIS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_FALLEN_PALM_RARE)
				.add(Biomes.DESERT)
				.addOptional(RegisterWorldgen.ARID_FOREST)
				.addOptional(RegisterWorldgen.ARID_SAVANNA)
				.addOptional(RegisterWorldgen.WARM_BEACH);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_FALLEN_PALM_AND_JUNGLE_AND_OAK)
				.add(Biomes.JUNGLE)
				.add(Biomes.BAMBOO_JUNGLE)
				.add(Biomes.SPARSE_JUNGLE)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_FALLEN_BIRCH_AND_OAK_DARK_FOREST)
				.add(Biomes.DARK_FOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_FALLEN_SPRUCE_TREES)
				.add(Biomes.TAIGA)
				.add(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
				.add(Biomes.OLD_GROWTH_PINE_TAIGA)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_CLEAN_FALLEN_SPRUCE_TREES)
				.add(Biomes.SNOWY_TAIGA)
				.addOptional(RegisterWorldgen.SNOWY_OLD_GROWTH_PINE_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_FALLEN_SWAMP_OAK_TREES)
				.add(Biomes.SWAMP);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_FALLEN_MANGROVE_TREES)
				.add(Biomes.MANGROVE_SWAMP);

			this.getOrCreateTagBuilder(WilderBiomeTags.CHERRY_TREES)
				.addOptional(Biomes.CHERRY_GROVE);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_MOSS_LAKE)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.RAINFOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_MOSS_LAKE_RARE)
				.addOptionalTag(BiomeTags.IS_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE);

			this.getOrCreateTagBuilder(WilderBiomeTags.FOREST_GRASS)
				.add(Biomes.FOREST)
				.add(Biomes.FLOWER_FOREST)
				.add(Biomes.BIRCH_FOREST)
				.add(Biomes.OLD_GROWTH_BIRCH_FOREST)
				.add(Biomes.DARK_FOREST)
				.add(Biomes.TAIGA)
				.add(Biomes.MANGROVE_SWAMP)
				.add(Biomes.SUNFLOWER_PLAINS)
				.addOptional(Biomes.CHERRY_GROVE)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.FLOWER_FIELD)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.PLAINS_GRASS)
				.add(Biomes.PLAINS)
				.add(Biomes.MEADOW);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_HUGE_RED_MUSHROOM)
				.add(Biomes.FOREST)
				.add(Biomes.FLOWER_FOREST)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_HUGE_BROWN_MUSHROOM)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_COMMON_BROWN_MUSHROOM)
				.add(Biomes.BIRCH_FOREST)
				.add(Biomes.DARK_FOREST)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_COMMON_RED_MUSHROOM)
				.add(Biomes.DARK_FOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_BIG_MUSHROOMS)
				.add(Biomes.BIRCH_FOREST)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_BIG_MUSHROOM_PATCH)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SWAMP_MUSHROOM)
				.add(Biomes.SWAMP);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SMALL_SPONGE)
				.add(Biomes.WARM_OCEAN);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SMALL_SPONGE_RARE)
				.add(Biomes.LUKEWARM_OCEAN)
				.add(Biomes.DEEP_LUKEWARM_OCEAN)
				.add(Biomes.LUSH_CAVES);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_CARNATION)
				.add(Biomes.BIRCH_FOREST)
				.add(Biomes.OLD_GROWTH_BIRCH_FOREST)
				.add(Biomes.FLOWER_FOREST)
				.add(Biomes.FOREST)
				.add(Biomes.DARK_FOREST)
				.add(Biomes.SPARSE_JUNGLE)
				.add(Biomes.JUNGLE)
				.add(Biomes.BAMBOO_JUNGLE)
				.add(Biomes.MANGROVE_SWAMP)
				.add(Biomes.MEADOW)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_DATURA)
				.add(Biomes.BIRCH_FOREST)
				.add(Biomes.OLD_GROWTH_BIRCH_FOREST)
				.addOptional(Biomes.CHERRY_GROVE)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_CATTAIL)
				.add(Biomes.DARK_FOREST)
				.add(Biomes.JUNGLE)
				.add(Biomes.BAMBOO_JUNGLE)
				.add(Biomes.SPARSE_JUNGLE)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_CATTAIL_UNCOMMON)
				.add(Biomes.OCEAN)
				.add(Biomes.DEEP_OCEAN)
				.add(Biomes.BEACH);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_CATTAIL_COMMON)
				.add(Biomes.SWAMP)
				.add(Biomes.MANGROVE_SWAMP)
				.add(Biomes.WARM_OCEAN)
				.add(Biomes.LUKEWARM_OCEAN)
				.add(Biomes.DEEP_LUKEWARM_OCEAN)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA)
				.addOptional(RegisterWorldgen.WARM_BEACH);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SEEDING_DANDELION)
				.add(Biomes.OLD_GROWTH_BIRCH_FOREST)
				.add(Biomes.FLOWER_FOREST)
				.add(Biomes.FOREST)
				.add(Biomes.MEADOW)
				.add(Biomes.WINDSWEPT_HILLS)
				.add(Biomes.WINDSWEPT_FOREST)
				.add(Biomes.DARK_FOREST)
				.addOptional(Biomes.CHERRY_GROVE)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_COMMON_SEEDING_DANDELION)
				.addOptional(Biomes.CHERRY_GROVE);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_RARE_SEEDING_DANDELION)
				.add(Biomes.PLAINS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_MILKWEED)
				.add(Biomes.BIRCH_FOREST)
				.add(Biomes.FLOWER_FOREST)
				.add(Biomes.FOREST)
				.add(Biomes.SWAMP)
				.add(Biomes.SPARSE_JUNGLE)
				.add(Biomes.JUNGLE)
				.add(Biomes.BAMBOO_JUNGLE)
				.add(Biomes.DARK_FOREST)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.CHERRY_FLOWERS)
				.addOptional(Biomes.CHERRY_GROVE);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_TUMBLEWEED_PLANT)
				.add(Biomes.DESERT)
				.add(Biomes.WINDSWEPT_SAVANNA)
				.add(Biomes.SAVANNA_PLATEAU)
				.addOptional(RegisterWorldgen.ARID_SAVANNA)
				.addOptionalTag(BiomeTags.IS_BADLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.SWAMP_TREES)
				.add(Biomes.SWAMP);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_PALMS)
				.add(Biomes.DESERT)
				.add(Biomes.BEACH)
				.add(Biomes.JUNGLE)
				.add(Biomes.SPARSE_JUNGLE)
				.addOptional(RegisterWorldgen.ARID_SAVANNA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_WARM_BEACH_PALMS)
				.addOptional(RegisterWorldgen.WARM_BEACH);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SHORT_SPRUCE)
				.add(Biomes.TAIGA)
				.add(Biomes.SNOWY_TAIGA)
				.add(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
				.add(Biomes.OLD_GROWTH_PINE_TAIGA)
				.add(Biomes.WINDSWEPT_FOREST)
				.add(Biomes.WINDSWEPT_HILLS)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.SNOWY_OLD_GROWTH_PINE_TAIGA)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_BIG_COARSE_SHRUB)
				.addOptionalTag(BiomeTags.IS_BADLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SNAPPED_OAK)
				.add(Biomes.FOREST)
				.add(Biomes.FLOWER_FOREST)
				.add(Biomes.DARK_FOREST)
				.add(Biomes.SWAMP)
				.addOptional(RegisterWorldgen.ARID_FOREST)
				.addOptional(RegisterWorldgen.PARCHED_FOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SNAPPED_BIRCH)
				.add(Biomes.BIRCH_FOREST)
				.add(Biomes.OLD_GROWTH_BIRCH_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SNAPPED_SPRUCE)
				.add(Biomes.TAIGA)
				.add(Biomes.SNOWY_TAIGA)
				.add(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
				.add(Biomes.OLD_GROWTH_PINE_TAIGA)
				.add(Biomes.WINDSWEPT_FOREST)
				.add(Biomes.WINDSWEPT_HILLS)
				.add(Biomes.GROVE)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.SNOWY_OLD_GROWTH_PINE_TAIGA)
				.addOptional(RegisterWorldgen.DARK_TAIGA)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SNAPPED_LARGE_SPRUCE)
				.add(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
				.add(Biomes.OLD_GROWTH_PINE_TAIGA)
				.addOptional(RegisterWorldgen.SNOWY_OLD_GROWTH_PINE_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SNAPPED_SPRUCE_SNOWY)
				.add(Biomes.SNOWY_TAIGA)
				.add(Biomes.GROVE)
				.addOptional(RegisterWorldgen.SNOWY_OLD_GROWTH_PINE_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SNAPPED_LARGE_SPRUCE_SNOWY)
				.add(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
				.add(Biomes.OLD_GROWTH_PINE_TAIGA)
				.addOptional(RegisterWorldgen.SNOWY_OLD_GROWTH_PINE_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SNAPPED_BIRCH_AND_OAK)
				.add(Biomes.FOREST)
				.add(Biomes.FLOWER_FOREST)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SNAPPED_BIRCH_AND_OAK_AND_SPRUCE)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SNAPPED_BIRCH_AND_SPRUCE)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SNAPPED_CYPRESS)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SNAPPED_JUNGLE)
				.add(Biomes.JUNGLE)
				.add(Biomes.BAMBOO_JUNGLE)
				.add(Biomes.SPARSE_JUNGLE);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SNAPPED_LARGE_JUNGLE)
				.add(Biomes.JUNGLE)
				.add(Biomes.BAMBOO_JUNGLE)
				.add(Biomes.SPARSE_JUNGLE);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SNAPPED_BIRCH_AND_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SNAPPED_ACACIA)
				.addOptional(RegisterWorldgen.ARID_SAVANNA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SNAPPED_ACACIA_AND_OAK)
				.add(Biomes.SAVANNA)
				.add(Biomes.SAVANNA_PLATEAU)
				.add(Biomes.WINDSWEPT_SAVANNA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SNAPPED_CHERRY)
				.addOptional(Biomes.CHERRY_GROVE);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SNAPPED_DARK_OAK)
				.add(Biomes.DARK_FOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_POLLEN)
				.add(Biomes.BIRCH_FOREST)
				.add(Biomes.OLD_GROWTH_BIRCH_FOREST)
				.add(Biomes.FOREST)
				.add(Biomes.FLOWER_FOREST)
				.add(Biomes.SUNFLOWER_PLAINS)
				.addOptional(Biomes.CHERRY_GROVE)
				.addOptional(RegisterWorldgen.FLOWER_FIELD)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.PARCHED_FOREST)
				.addOptional(RegisterWorldgen.RAINFOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_FIELD_FLOWERS)
				.add(Biomes.FLOWER_FOREST)
				.addOptional(RegisterWorldgen.FLOWER_FIELD);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SUNFLOWER_PLAINS_FLOWERS)
				.add(Biomes.SUNFLOWER_PLAINS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SHORT_MEGA_SPRUCE)
				.add(Biomes.TAIGA)
				.add(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
				.add(Biomes.OLD_GROWTH_PINE_TAIGA)
				.add(Biomes.SNOWY_TAIGA)
				.add(Biomes.GROVE)
				.addOptional(RegisterWorldgen.SNOWY_OLD_GROWTH_PINE_TAIGA)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SHORT_MEGA_SPRUCE_SNOWY)
				.add(Biomes.SNOWY_TAIGA)
				.add(Biomes.GROVE)
				.addOptional(RegisterWorldgen.SNOWY_OLD_GROWTH_PINE_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_RED_SHELF_FUNGUS)
				.add(Biomes.DARK_FOREST)
				.add(Biomes.FOREST)
				.add(Biomes.OLD_GROWTH_BIRCH_FOREST)
				.add(Biomes.BIRCH_FOREST)
				.add(Biomes.PLAINS)
				.add(Biomes.FLOWER_FOREST)
				.add(Biomes.SUNFLOWER_PLAINS)
				.addOptional(Biomes.CHERRY_GROVE)
				.addOptional(RegisterWorldgen.FLOWER_FIELD)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_BROWN_SHELF_FUNGUS)
				.add(Biomes.DARK_FOREST)
				.add(Biomes.FOREST)
				.add(Biomes.OLD_GROWTH_BIRCH_FOREST)
				.add(Biomes.BIRCH_FOREST)
				.add(Biomes.PLAINS)
				.add(Biomes.FLOWER_FOREST)
				.add(Biomes.SUNFLOWER_PLAINS)
				.add(Biomes.SWAMP)
				.add(Biomes.MANGROVE_SWAMP)
				.add(Biomes.TAIGA)
				.add(Biomes.SNOWY_TAIGA)
				.addOptional(RegisterWorldgen.FLOWER_FIELD)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.PARCHED_FOREST)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.SNOWY_OLD_GROWTH_PINE_TAIGA)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_RAINFOREST_MUSHROOM)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_MIXED_MUSHROOM)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_GLORY_OF_THE_SNOW)
				.add(Biomes.BIRCH_FOREST)
				.add(Biomes.OLD_GROWTH_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_FLOWERING_WATER_LILY)
				.add(Biomes.SWAMP)
				.add(Biomes.MANGROVE_SWAMP);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_BERRY_PATCH)
				.add(Biomes.FLOWER_FOREST)
				.addOptional(RegisterWorldgen.FLOWER_FIELD)
				.addOptional(RegisterWorldgen.MIXED_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_BUSH)
				.addOptional(Biomes.CHERRY_GROVE);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_FOREST_SHRUB)
				.add(Biomes.FOREST)
				.add(Biomes.FLOWER_FOREST)
				.add(Biomes.DARK_FOREST)
				.addOptional(RegisterWorldgen.ARID_FOREST)
				.addOptional(RegisterWorldgen.PARCHED_FOREST)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SHRUB)
				.add(Biomes.WINDSWEPT_SAVANNA)
				.add(Biomes.SAVANNA_PLATEAU)
				.add(Biomes.SAVANNA)
				.add(Biomes.SUNFLOWER_PLAINS)
				.addOptional(RegisterWorldgen.FLOWER_FIELD)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_PLAINS_FLOWERS)
				.add(Biomes.PLAINS)
				.add(Biomes.MEADOW)
				.add(Biomes.FOREST)
				.add(Biomes.BIRCH_FOREST)
				.add(Biomes.OLD_GROWTH_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_CYPRESS_FLOWERS)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_RARE_MILKWEED)
				.add(Biomes.PLAINS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_LARGE_FERN_AND_GRASS)
				.add(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
				.add(Biomes.OLD_GROWTH_PINE_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_LARGE_FERN_AND_GRASS_RARE)
				.add(Biomes.TAIGA)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_NEW_RARE_GRASS)
				.add(Biomes.WINDSWEPT_FOREST)
				.add(Biomes.WINDSWEPT_HILLS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_FLOWER_FIELD_TALL_GRASS)
				.add(Biomes.PLAINS)
				.add(Biomes.SUNFLOWER_PLAINS)
				.addOptional(RegisterWorldgen.FLOWER_FIELD);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_DENSE_FERN)
				.add(Biomes.MANGROVE_SWAMP)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_DENSE_TALL_GRASS)
				.add(Biomes.MANGROVE_SWAMP)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SPARSE_JUNGLE_FLOWERS)
				.add(Biomes.MANGROVE_SWAMP)
				.add(Biomes.SPARSE_JUNGLE)
				.add(Biomes.BAMBOO_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_JUNGLE_FLOWERS)
				.add(Biomes.JUNGLE)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_JUNGLE_BUSH)
				.add(Biomes.JUNGLE)
				.add(Biomes.BAMBOO_JUNGLE)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SPARSE_BUSH)
				.add(Biomes.SUNFLOWER_PLAINS)
				.add(Biomes.SPARSE_JUNGLE)
				.add(Biomes.BAMBOO_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_ARID_BUSH)
				.add(Biomes.SAVANNA)
				.add(Biomes.SAVANNA_PLATEAU)
				.add(Biomes.WINDSWEPT_SAVANNA)
				.addOptional(RegisterWorldgen.ARID_FOREST)
				.addOptional(RegisterWorldgen.ARID_SAVANNA)
				.addOptional(RegisterWorldgen.PARCHED_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_FLOWER_FIELD_BUSH)
				.addOptional(RegisterWorldgen.FLOWER_FIELD);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_RAINFOREST_BUSH)
				.add(Biomes.MANGROVE_SWAMP)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_BADLANDS_SAND_BUSH)
				.add(Biomes.BADLANDS)
				.add(Biomes.WOODED_BADLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_BADLANDS_TERRACOTTA_BUSH)
				.add(Biomes.BADLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_WOODED_BADLANDS_TERRACOTTA_BUSH)
				.add(Biomes.WOODED_BADLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_BADLANDS_RARE_SAND_BUSH)
				.add(Biomes.ERODED_BADLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_DESERT_BUSH)
				.add(Biomes.DESERT);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_OASIS_BUSH)
				.addOptional(RegisterWorldgen.OASIS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_TALL_CACTUS)
				.add(Biomes.DESERT);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_PRICKLY_PEAR)
				.add(Biomes.BADLANDS)
				.add(Biomes.WOODED_BADLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_RARE_PRICKLY_PEAR)
				.add(Biomes.ERODED_BADLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_TALL_BADLANDS_CACTUS)
				.add(Biomes.BADLANDS)
				.add(Biomes.WOODED_BADLANDS)
				.add(Biomes.ERODED_BADLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_MOSS_PILE)
				.add(Biomes.SPARSE_JUNGLE)
				.add(Biomes.BAMBOO_JUNGLE)
				.add(Biomes.JUNGLE)
				.add(Biomes.SWAMP)
				.addOptional(Biomes.MANGROVE_SWAMP)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_DECORATIVE_MUD)
				.add(Biomes.SWAMP);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_PACKED_MUD_ORE)
				.add(Biomes.WINDSWEPT_SAVANNA)
				.add(Biomes.DESERT)
				.addOptional(RegisterWorldgen.OASIS)
				.addOptional(RegisterWorldgen.ARID_SAVANNA)
				.addOptional(RegisterWorldgen.ARID_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_COARSE_DIRT_PATH)
				.add(Biomes.TAIGA)
				.add(Biomes.OLD_GROWTH_PINE_TAIGA)
				.add(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
				.add(Biomes.WINDSWEPT_FOREST)
				.addOptional(RegisterWorldgen.ARID_SAVANNA)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_COARSE_DIRT_PATH_SMALL)
				.addOptionalTag(BiomeTags.IS_BADLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_PACKED_MUD_PATH_BADLANDS)
				.addOptionalTag(BiomeTags.IS_BADLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SANDSTONE_PATH)
				.add(Biomes.DESERT)
				.addOptional(RegisterWorldgen.ARID_SAVANNA)
				.addOptional(RegisterWorldgen.ARID_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_COARSE_DIRT_CLEARING)
				.add(Biomes.FOREST)
				.add(Biomes.FLOWER_FOREST)
				.add(Biomes.BIRCH_FOREST)
				.add(Biomes.OLD_GROWTH_BIRCH_FOREST)
				.add(Biomes.DARK_FOREST)
				.add(Biomes.TAIGA)
				.add(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
				.add(Biomes.OLD_GROWTH_PINE_TAIGA)
				.add(Biomes.SNOWY_TAIGA)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.PARCHED_FOREST)
				.addOptional(RegisterWorldgen.ARID_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_ROOTED_DIRT_CLEARING)
				.add(Biomes.FOREST)
				.add(Biomes.FLOWER_FOREST)
				.add(Biomes.BIRCH_FOREST)
				.add(Biomes.OLD_GROWTH_BIRCH_FOREST)
				.add(Biomes.TAIGA)
				.add(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
				.add(Biomes.OLD_GROWTH_PINE_TAIGA)
				.add(Biomes.SNOWY_TAIGA)
				.add(Biomes.DARK_FOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.PARCHED_FOREST)
				.addOptional(RegisterWorldgen.ARID_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_GRAVEL_CLEARING)
				.add(Biomes.TAIGA)
				.add(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
				.add(Biomes.OLD_GROWTH_PINE_TAIGA)
				.add(Biomes.SNOWY_TAIGA)
				.addOptional(RegisterWorldgen.DARK_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.MIXED_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_BIRCH_CLEARING_FLOWERS)
				.add(Biomes.BIRCH_FOREST)
				.add(Biomes.OLD_GROWTH_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_FOREST_CLEARING_FLOWERS)
				.add(Biomes.FOREST)
				.add(Biomes.FLOWER_FOREST)
				.add(Biomes.DARK_FOREST)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.RAINFOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SCORCHED_SAND)
				.add(Biomes.DESERT)
				.addOptional(RegisterWorldgen.OASIS)
				.addOptional(RegisterWorldgen.ARID_SAVANNA)
				.addOptional(RegisterWorldgen.ARID_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SCORCHED_RED_SAND)
				.addOptionalTag(BiomeTags.IS_BADLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SMALL_SAND_TRANSITION)
				.add(Biomes.SNOWY_BEACH)
				.add(Biomes.BEACH)
				.addOptional(RegisterWorldgen.WARM_RIVER)
				.addOptional(RegisterWorldgen.WARM_BEACH);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_SAND_TRANSITION)
				.add(Biomes.DESERT)
				.addOptional(RegisterWorldgen.OASIS)
				.addOptional(RegisterWorldgen.ARID_SAVANNA)
				.addOptional(RegisterWorldgen.ARID_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_RED_SAND_TRANSITION)
				.addOptionalTag(BiomeTags.IS_BADLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_STONE_TRANSITION)
				.add(Biomes.STONY_PEAKS)
				.add(Biomes.STONY_SHORE)
				.add(Biomes.WINDSWEPT_GRAVELLY_HILLS)
				.add(Biomes.WINDSWEPT_HILLS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_STONE_TRANSITION)
				.add(Biomes.STONY_PEAKS)
				.add(Biomes.STONY_SHORE)
				.add(Biomes.WINDSWEPT_GRAVELLY_HILLS)
				.add(Biomes.WINDSWEPT_HILLS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_BETA_BEACH_SAND_TRANSITION)
				.addOptionalTag(WilderBiomeTags.SAND_BEACHES)
				.addOptionalTag(WilderBiomeTags.MULTI_LAYER_SAND_BEACHES);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_BETA_BEACH_GRAVEL_TRANSITION)
				.addOptionalTag(WilderBiomeTags.GRAVEL_BEACH);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_GRAVEL_TRANSITION)
				.add(Biomes.WINDSWEPT_GRAVELLY_HILLS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_MUD_TRANSITION)
				.add(Biomes.MANGROVE_SWAMP)
				.add(Biomes.SWAMP);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_TERMITE_MOUND)
				.addOptionalTag(BiomeTags.IS_SAVANNA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_TAIGA_FOREST_ROCK)
				.add(Biomes.TAIGA)
				.add(Biomes.SNOWY_TAIGA)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_MOSS_PATH)
				.add(Biomes.JUNGLE)
				.add(Biomes.SPARSE_JUNGLE)
				.add(Biomes.BAMBOO_JUNGLE)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.OASIS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_PACKED_MUD_PATH)
				.add(Biomes.SAVANNA)
				.add(Biomes.SAVANNA_PLATEAU)
				.add(Biomes.SAVANNA_PLATEAU)
				.addOptional(RegisterWorldgen.ARID_SAVANNA)
				.addOptional(RegisterWorldgen.PARCHED_FOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_MUD_BASIN)
				.add(Biomes.MANGROVE_SWAMP);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_MUD_PILE)
				.add(Biomes.MANGROVE_SWAMP);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_MUD_LAKE)
				.add(Biomes.MANGROVE_SWAMP);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_ALGAE_SMALL)
				.addOptionalTag(BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_ALGAE)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_WATER_POOLS)
				.add(Biomes.RIVER)
				.addOptionalTag(BiomeTags.IS_OCEAN)
				.addOptionalTag(ConventionalBiomeTags.OCEAN);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_WATER_SHRUBS)
				.add(Biomes.RIVER)
				.addOptionalTag(BiomeTags.IS_OCEAN)
				.addOptionalTag(ConventionalBiomeTags.OCEAN);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_WATER_GRASS)
				.add(Biomes.RIVER)
				.addOptionalTag(BiomeTags.IS_OCEAN)
				.addOptionalTag(ConventionalBiomeTags.OCEAN);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_MOSS_BASIN)
				.add(Biomes.JUNGLE)
				.add(Biomes.SPARSE_JUNGLE)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_PODZOL_BASIN)
				.add(Biomes.BAMBOO_JUNGLE)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_MOSS_CARPET)
				.add(Biomes.MANGROVE_SWAMP)
				.add(Biomes.SWAMP)
				.add(Biomes.JUNGLE)
				.add(Biomes.SPARSE_JUNGLE)
				.add(Biomes.BAMBOO_JUNGLE)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_RARE_COARSE)
				.add(Biomes.FOREST)
				.add(Biomes.SAVANNA)
				.add(Biomes.DARK_FOREST)
				.addOptional(RegisterWorldgen.ARID_SAVANNA)
				.addOptional(RegisterWorldgen.ARID_FOREST)
				.addOptional(RegisterWorldgen.PARCHED_FOREST)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_RARE_GRAVEL)
				.add(Biomes.FOREST)
				.add(Biomes.BIRCH_FOREST)
				.add(Biomes.OLD_GROWTH_BIRCH_FOREST)
				.add(Biomes.DARK_FOREST)
				.add(Biomes.TAIGA)
				.add(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
				.add(Biomes.OLD_GROWTH_PINE_TAIGA)
				.add(Biomes.FLOWER_FOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.FLOWER_FIELD)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE);

			this.getOrCreateTagBuilder(WilderBiomeTags.HAS_RARE_STONE)
				.add(Biomes.FOREST)
				.add(Biomes.BIRCH_FOREST)
				.add(Biomes.OLD_GROWTH_BIRCH_FOREST)
				.add(Biomes.DARK_FOREST)
				.add(Biomes.TAIGA)
				.add(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
				.add(Biomes.OLD_GROWTH_PINE_TAIGA)
				.add(Biomes.FLOWER_FOREST)
				.add(Biomes.SAVANNA_PLATEAU)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.FLOWER_FIELD);
		}

		private void generateStructureTags() {
			this.getOrCreateTagBuilder(WilderBiomeTags.ABANDONED_CABIN_HAS_STRUCTURE)
				.addOptionalTag(BiomeTags.IS_OCEAN)
				.addOptionalTag(BiomeTags.IS_RIVER)
				.addOptionalTag(BiomeTags.IS_MOUNTAIN)
				.addOptionalTag(BiomeTags.IS_HILL)
				.addOptionalTag(BiomeTags.IS_TAIGA)
				.addOptionalTag(BiomeTags.IS_JUNGLE)
				.addOptionalTag(BiomeTags.IS_FOREST)
				.addOptional(Biomes.CHERRY_GROVE)
				.add(Biomes.STONY_SHORE)
				.add(Biomes.MUSHROOM_FIELDS)
				.add(Biomes.ICE_SPIKES)
				.add(Biomes.WINDSWEPT_SAVANNA)
				.add(Biomes.DESERT)
				.add(Biomes.SAVANNA)
				.add(Biomes.SNOWY_PLAINS)
				.add(Biomes.PLAINS)
				.add(Biomes.SUNFLOWER_PLAINS)
				.add(Biomes.SWAMP)
				.add(Biomes.MANGROVE_SWAMP)
				.add(Biomes.SAVANNA_PLATEAU)
				.add(Biomes.DRIPSTONE_CAVES)
				.add(Biomes.DEEP_DARK)
				.addOptionalTag(WilderBiomeTags.WILDER_WILD_BIOMES);

			this.getOrCreateTagBuilder(BiomeTags.HAS_DESERT_PYRAMID)
				.addOptional(RegisterWorldgen.OASIS);

			this.getOrCreateTagBuilder(BiomeTags.HAS_IGLOO)
				.addOptional(RegisterWorldgen.SNOWY_OLD_GROWTH_PINE_TAIGA);

			this.getOrCreateTagBuilder(BiomeTags.HAS_MINESHAFT)
				.addOptionalTag(WilderBiomeTags.WILDER_WILD_BIOMES);

			this.getOrCreateTagBuilder(BiomeTags.HAS_PILLAGER_OUTPOST)
				.add(Biomes.SUNFLOWER_PLAINS)
				.add(Biomes.SNOWY_TAIGA)
				.add(Biomes.ICE_SPIKES)
				.addOptional(RegisterWorldgen.OASIS)
				.addOptional(RegisterWorldgen.FLOWER_FIELD)
				.addOptional(RegisterWorldgen.ARID_SAVANNA)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.ARID_FOREST)
				.addOptional(RegisterWorldgen.PARCHED_FOREST);

			this.getOrCreateTagBuilder(BiomeTags.HAS_RUINED_PORTAL_DESERT)
				.addOptional(RegisterWorldgen.OASIS)
				.addOptional(RegisterWorldgen.ARID_SAVANNA)
				.addOptional(RegisterWorldgen.ARID_FOREST);

			this.getOrCreateTagBuilder(BiomeTags.HAS_RUINED_PORTAL_STANDARD)
				.addOptional(RegisterWorldgen.FLOWER_FIELD)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.DARK_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST)
				.addOptional(RegisterWorldgen.MIXED_FOREST)
				.addOptional(RegisterWorldgen.PARCHED_FOREST)
				.addOptional(RegisterWorldgen.SEMI_BIRCH_FOREST)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.DARK_TAIGA);

			this.getOrCreateTagBuilder(BiomeTags.HAS_RUINED_PORTAL_JUNGLE)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE);

			this.getOrCreateTagBuilder(BiomeTags.HAS_RUINED_PORTAL_OCEAN)
				.addOptional(RegisterWorldgen.JELLYFISH_CAVES);

			this.getOrCreateTagBuilder(BiomeTags.HAS_RUINED_PORTAL_SWAMP)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS);

			this.getOrCreateTagBuilder(BiomeTags.HAS_VILLAGE_PLAINS)
				.add(Biomes.SUNFLOWER_PLAINS)
				.addOptional(RegisterWorldgen.FLOWER_FIELD);

			this.getOrCreateTagBuilder(BiomeTags.HAS_VILLAGE_SAVANNA)
				.addOptional(RegisterWorldgen.ARID_SAVANNA);

			this.getOrCreateTagBuilder(BiomeTags.HAS_VILLAGE_SNOWY)
				.add(Biomes.ICE_SPIKES)
				.add(Biomes.SNOWY_TAIGA);

			this.getOrCreateTagBuilder(BiomeTags.HAS_WOODLAND_MANSION)
				.addOptional(RegisterWorldgen.OLD_GROWTH_DARK_FOREST);

			this.getOrCreateTagBuilder(BiomeTags.HAS_TRAIL_RUINS)
				.addOptional(RegisterWorldgen.CYPRESS_WETLANDS)
				.addOptional(RegisterWorldgen.BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.SPARSE_BIRCH_JUNGLE)
				.addOptional(RegisterWorldgen.BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.TEMPERATE_RAINFOREST)
				.addOptional(RegisterWorldgen.RAINFOREST)
				.addOptional(RegisterWorldgen.OLD_GROWTH_BIRCH_TAIGA)
				.addOptional(RegisterWorldgen.SNOWY_OLD_GROWTH_PINE_TAIGA)
				.addOptional(RegisterWorldgen.DARK_TAIGA);
		}
	}

	private static final class WilderBlockLootProvider extends FabricBlockLootTableProvider {

		private WilderBlockLootProvider(@NotNull FabricDataOutput dataOutput) {
			super(dataOutput);
		}

		@Override
		public void generate() {
			this.dropSelf(RegisterBlocks.BAOBAB_HANGING_SIGN);
			this.dropSelf(RegisterBlocks.CYPRESS_HANGING_SIGN);
			this.dropSelf(RegisterBlocks.PALM_HANGING_SIGN);
		}
	}

	private static final class WilderBlockTagProvider extends FabricTagProvider.BlockTagProvider {
		public WilderBlockTagProvider(@NotNull FabricDataOutput output, @NotNull CompletableFuture completableFuture) {
			super(output, completableFuture);
		}

		@Override
		protected void addTags(@NotNull HolderLookup.Provider arg) {
			this.generateFeatures();
			this.generateDeepDark();
			this.generateHollowedAndTermites();
			this.generateTags();
		}

		private void generateFeatures() {
			this.getOrCreateTagBuilder(WilderBlockTags.STONE_TRANSITION_REPLACEABLE)
				.add(Blocks.GRASS_BLOCK)
				.add(Blocks.DIRT)
				.add(Blocks.MUD)
				.add(Blocks.SAND);

			this.getOrCreateTagBuilder(WilderBlockTags.STONE_TRANSITION_PLACEABLE)
				.add(Blocks.STONE);

			this.getOrCreateTagBuilder(WilderBlockTags.SMALL_SAND_TRANSITION_REPLACEABLE)
				.add(Blocks.GRASS_BLOCK)
				.add(Blocks.DIRT)
				.add(Blocks.MUD);

			this.getOrCreateTagBuilder(WilderBlockTags.GRAVEL_TRANSITION_REPLACEABLE)
				.add(Blocks.GRASS_BLOCK)
				.add(Blocks.DIRT)
				.add(Blocks.MUD)
				.add(Blocks.SAND)
				.add(Blocks.STONE);

			this.getOrCreateTagBuilder(WilderBlockTags.GRAVEL_TRANSITION_PLACEABLE)
				.add(Blocks.GRAVEL);

			this.getOrCreateTagBuilder(WilderBlockTags.SAND_TRANSITION_REPLACEABLE)
				.add(Blocks.GRAVEL)
				.add(Blocks.GRASS_BLOCK)
				.add(Blocks.STONE)
				.add(Blocks.DIRT)
				.add(Blocks.MUD);

			this.getOrCreateTagBuilder(WilderBlockTags.SAND_TRANSITION_PLACEABLE)
				.add(Blocks.SAND);

			this.getOrCreateTagBuilder(WilderBlockTags.RED_SAND_TRANSITION_REPLACEABLE)
				.add(Blocks.GRASS_BLOCK)
				.add(Blocks.GRAVEL)
				.add(Blocks.MUD)
				.add(Blocks.SAND)
				.addOptionalTag(BlockTags.BASE_STONE_OVERWORLD)
				.addOptionalTag(BlockTags.LEAVES);

			this.getOrCreateTagBuilder(WilderBlockTags.RED_SAND_TRANSITION_PLACEABLE)
				.add(Blocks.RED_SAND)
				.addOptionalTag(BlockTags.TERRACOTTA);

			this.getOrCreateTagBuilder(WilderBlockTags.MUD_TRANSITION_REPLACEABLE)
				.add(Blocks.DIRT)
				.add(Blocks.GRASS_BLOCK)
				.add(Blocks.CLAY)
				.add(Blocks.SAND);

			this.getOrCreateTagBuilder(WilderBlockTags.MUD_TRANSITION_PLACEABLE)
				.add(Blocks.MUD)
				.add(Blocks.MUDDY_MANGROVE_ROOTS);

			this.getOrCreateTagBuilder(WilderBlockTags.MUD_PATH_REPLACEABLE)
				.add(Blocks.DIRT)
				.add(Blocks.GRASS_BLOCK)
				.add(Blocks.CLAY)
				.add(Blocks.SAND);

			this.getOrCreateTagBuilder(WilderBlockTags.COARSE_PATH_REPLACEABLE)
				.add(Blocks.DIRT)
				.add(Blocks.GRASS_BLOCK)
				.add(Blocks.PODZOL);

			this.getOrCreateTagBuilder(WilderBlockTags.COARSE_CLEARING_REPLACEABLE)
				.add(Blocks.DIRT)
				.add(Blocks.GRASS_BLOCK)
				.add(Blocks.PODZOL)
				.add(Blocks.GRAVEL);

			this.getOrCreateTagBuilder(WilderBlockTags.ROOTED_DIRT_PATH_REPLACEABLE)
				.add(Blocks.GRAVEL)
				.add(Blocks.COARSE_DIRT);

			this.getOrCreateTagBuilder(WilderBlockTags.UNDER_WATER_SAND_PATH_REPLACEABLE)
				.add(Blocks.DIRT)
				.add(Blocks.GRASS_BLOCK)
				.add(Blocks.GRAVEL);

			this.getOrCreateTagBuilder(WilderBlockTags.UNDER_WATER_GRAVEL_PATH_REPLACEABLE)
				.add(Blocks.DIRT)
				.add(Blocks.GRASS_BLOCK)
				.add(Blocks.STONE);

			this.getOrCreateTagBuilder(WilderBlockTags.UNDER_WATER_CLAY_PATH_REPLACEABLE)
				.add(Blocks.DIRT)
				.add(Blocks.GRASS_BLOCK)
				.add(Blocks.GRAVEL)
				.add(Blocks.STONE);

			this.getOrCreateTagBuilder(WilderBlockTags.BEACH_CLAY_PATH_REPLACEABLE)
				.add(Blocks.SAND);

			this.getOrCreateTagBuilder(WilderBlockTags.RIVER_GRAVEL_PATH_REPLACEABLE)
				.add(Blocks.SAND);

			this.getOrCreateTagBuilder(WilderBlockTags.SAND_PATH_REPLACEABLE)
				.add(Blocks.DIRT)
				.add(Blocks.GRASS_BLOCK)
				.add(Blocks.GRAVEL)
				.addOptionalTag(BlockTags.BASE_STONE_OVERWORLD);

			this.getOrCreateTagBuilder(WilderBlockTags.GRAVEL_PATH_REPLACEABLE)
				.add(Blocks.DIRT)
				.add(Blocks.GRASS_BLOCK)
				.add(Blocks.COARSE_DIRT)
				.addOptionalTag(BlockTags.BASE_STONE_OVERWORLD);

			this.getOrCreateTagBuilder(WilderBlockTags.GRAVEL_CLEARING_REPLACEABLE)
				.add(Blocks.DIRT)
				.add(Blocks.GRASS_BLOCK)
				.add(Blocks.COARSE_DIRT)
				.addOptionalTag(BlockTags.BASE_STONE_OVERWORLD);

			this.getOrCreateTagBuilder(WilderBlockTags.STONE_PATH_REPLACEABLE)
				.add(Blocks.DIRT)
				.add(Blocks.GRASS_BLOCK)
				.addOptionalTag(BlockTags.SAND);

			this.getOrCreateTagBuilder(WilderBlockTags.PACKED_MUD_PATH_REPLACEABLE)
				.add(Blocks.DIRT)
				.add(Blocks.GRASS_BLOCK)
				.add(Blocks.COARSE_DIRT);

			this.getOrCreateTagBuilder(WilderBlockTags.MOSS_PATH_REPLACEABLE)
				.add(Blocks.GRASS_BLOCK)
				.add(Blocks.PODZOL);

			this.getOrCreateTagBuilder(WilderBlockTags.SANDSTONE_PATH_REPLACEABLE)
				.add(Blocks.SAND);

			this.getOrCreateTagBuilder(WilderBlockTags.SMALL_COARSE_DIRT_PATH_REPLACEABLE)
				.add(Blocks.RED_SAND);

			this.getOrCreateTagBuilder(WilderBlockTags.PACKED_MUD_PATH_BADLANDS_REPLACEABLE)
				.add(Blocks.RED_SAND)
				.add(Blocks.RED_SANDSTONE)
				.addOptionalTag(BlockTags.TERRACOTTA);

			this.getOrCreateTagBuilder(WilderBlockTags.POLLEN_FEATURE_PLACEABLE)
				.add(Blocks.GRASS_BLOCK)
				.addOptionalTag(BlockTags.LEAVES)
				.addOptionalTag(BlockTags.OVERWORLD_NATURAL_LOGS);

			this.getOrCreateTagBuilder(WilderBlockTags.TERMITE_DISC_REPLACEABLE)
				.addOptionalTag(BlockTags.DIRT)
				.addOptionalTag(BlockTags.SAND)
				.addOptionalTag(BlockTags.BASE_STONE_OVERWORLD);

			this.getOrCreateTagBuilder(WilderBlockTags.TERMITE_DISC_BLOCKS)
				.add(Blocks.COARSE_DIRT)
				.add(Blocks.SAND)
				.add(Blocks.PACKED_MUD);

			this.getOrCreateTagBuilder(WilderBlockTags.BLUE_NEMATOCYST_FEATURE_PLACEABLE)
				.add(Blocks.CLAY)
				.add(Blocks.DRIPSTONE_BLOCK)
				.add(Blocks.CALCITE)
				.addOptional(WilderSharedConstants.id("blue_pearlescent_mesoglea"))
				.addOptionalTag(BlockTags.BASE_STONE_OVERWORLD);

			this.getOrCreateTagBuilder(WilderBlockTags.PURPLE_NEMATOCYST_FEATURE_PLACEABLE)
				.add(Blocks.CLAY)
				.add(Blocks.DRIPSTONE_BLOCK)
				.add(Blocks.CALCITE)
				.addOptional(WilderSharedConstants.id("purple_pearlescent_mesoglea"))
				.addOptionalTag(BlockTags.BASE_STONE_OVERWORLD);

			this.getOrCreateTagBuilder(WilderBlockTags.SHELF_FUNGUS_FEATURE_PLACEABLE)
				.add(Blocks.MUSHROOM_STEM)
				.addOptionalTag(BlockTags.OVERWORLD_NATURAL_LOGS);

			this.getOrCreateTagBuilder(WilderBlockTags.SCORCHED_SAND_FEATURE_INNER_REPLACEABLE)
				.add(Blocks.SAND)
				.addOptional(WilderSharedConstants.id("scorched_sand"));

			this.getOrCreateTagBuilder(WilderBlockTags.SCORCHED_SAND_FEATURE_REPLACEABLE)
				.add(Blocks.SAND);

			this.getOrCreateTagBuilder(WilderBlockTags.RED_SCORCHED_SAND_FEATURE_INNER_REPLACEABLE)
				.add(Blocks.RED_SAND)
				.addOptional(WilderSharedConstants.id("red_scorched_sand"));

			this.getOrCreateTagBuilder(WilderBlockTags.RED_SCORCHED_SAND_FEATURE_REPLACEABLE)
				.add(Blocks.RED_SAND);

			this.getOrCreateTagBuilder(WilderBlockTags.MESOGLEA_PATH_REPLACEABLE)
				.add(Blocks.CLAY)
				.add(Blocks.DRIPSTONE_BLOCK)
				.add(Blocks.CALCITE)
				.addOptionalTag(BlockTags.BASE_STONE_OVERWORLD);

			this.getOrCreateTagBuilder(WilderBlockTags.OASIS_PATH_REPLACEABLE)
				.add(Blocks.SAND)
				.add(Blocks.SANDSTONE);

			this.getOrCreateTagBuilder(WilderBlockTags.RIVER_POOL_REPLACEABLE)
				.addOptionalTag(BlockTags.SAND)
				.addOptionalTag(BlockTags.DIRT)
				.add(Blocks.GRAVEL)
				.add(Blocks.CLAY);
		}

		private void generateTags() {
			this.getOrCreateTagBuilder(WilderBlockTags.STOPS_TUMBLEWEED)
				.add(Blocks.MUD)
				.add(Blocks.MUDDY_MANGROVE_ROOTS)
				.add(Blocks.SLIME_BLOCK)
				.add(Blocks.IRON_BARS)
				.add(Blocks.HONEY_BLOCK);

			this.getOrCreateTagBuilder(WilderBlockTags.SPLITS_COCONUT)
				.addOptionalTag(BlockTags.MINEABLE_WITH_PICKAXE)
				.addOptionalTag(BlockTags.BASE_STONE_OVERWORLD)
				.addOptionalTag(BlockTags.BASE_STONE_NETHER)
				.addOptionalTag(BlockTags.DRAGON_IMMUNE)
				.addOptionalTag(BlockTags.WITHER_IMMUNE)
				.addOptionalTag(BlockTags.LOGS);

			this.getOrCreateTagBuilder(WilderBlockTags.CATTAIL_PLACEABLE)
				.addOptionalTag(BlockTags.DIRT)
				.addOptionalTag(BlockTags.SAND)
				.add(Blocks.CLAY);

			this.getOrCreateTagBuilder(WilderBlockTags.CATTAIL_MUD_PLACEABLE)
				.add(Blocks.MUD);

			this.getOrCreateTagBuilder(BlockTags.SAND)
				.add(RegisterBlocks.SCORCHED_SAND)
				.add(RegisterBlocks.SCORCHED_RED_SAND);
		}

		private void generateDeepDark() {
			this.getOrCreateTagBuilder(WilderBlockTags.ANCIENT_CITY_BLOCKS)
				.add(Blocks.COBBLED_DEEPSLATE)
				.add(Blocks.COBBLED_DEEPSLATE_STAIRS)
				.add(Blocks.COBBLED_DEEPSLATE_SLAB)
				.add(Blocks.COBBLED_DEEPSLATE_WALL)
				.add(Blocks.POLISHED_DEEPSLATE)
				.add(Blocks.POLISHED_DEEPSLATE_STAIRS)
				.add(Blocks.POLISHED_DEEPSLATE_SLAB)
				.add(Blocks.POLISHED_DEEPSLATE_WALL)
				.add(Blocks.DEEPSLATE_BRICKS)
				.add(Blocks.DEEPSLATE_BRICK_STAIRS)
				.add(Blocks.DEEPSLATE_BRICK_SLAB)
				.add(Blocks.DEEPSLATE_BRICK_WALL)
				.add(Blocks.CRACKED_DEEPSLATE_BRICKS)
				.add(Blocks.DEEPSLATE_TILES)
				.add(Blocks.DEEPSLATE_TILE_STAIRS)
				.add(Blocks.CHISELED_DEEPSLATE)
				.add(Blocks.REINFORCED_DEEPSLATE)
				.add(Blocks.POLISHED_BASALT)
				.add(Blocks.SMOOTH_BASALT)
				.add(Blocks.DARK_OAK_LOG)
				.add(Blocks.DARK_OAK_PLANKS)
				.add(Blocks.DARK_OAK_FENCE)
				.add(Blocks.LIGHT_BLUE_CARPET)
				.add(Blocks.BLUE_CARPET)
				.add(Blocks.LIGHT_BLUE_WOOL)
				.add(Blocks.GRAY_WOOL)
				.add(Blocks.CHEST)
				.add(Blocks.LADDER)
				.add(Blocks.CANDLE)
				.add(Blocks.WHITE_CANDLE)
				.add(Blocks.SOUL_LANTERN)
				.add(Blocks.SOUL_FIRE)
				.add(Blocks.SOUL_SAND);

			this.getOrCreateTagBuilder(WilderBlockTags.ANCIENT_HORN_NON_COLLIDE)
				.add(Blocks.SCULK)
				.add(RegisterBlocks.OSSEOUS_SCULK)
				.addOptionalTag(ConventionalBlockTags.GLASS_BLOCKS)
				.addOptionalTag(ConventionalBlockTags.GLASS_PANES)
				.addOptionalTag(BlockTags.LEAVES)
				.add(Blocks.BELL)
				.add(Blocks.POINTED_DRIPSTONE)
				.add(Blocks.BAMBOO)
				.add(Blocks.ICE)
				.add(RegisterBlocks.SCULK_STAIRS)
				.add(RegisterBlocks.SCULK_SLAB)
				.add(RegisterBlocks.SCULK_WALL);

			this.getOrCreateTagBuilder(WilderBlockTags.SCULK_SLAB_REPLACEABLE)
				.add(Blocks.STONE_SLAB)
				.add(Blocks.GRANITE_SLAB)
				.add(Blocks.DIORITE_SLAB)
				.add(Blocks.ANDESITE_SLAB)
				.add(Blocks.BLACKSTONE_SLAB);

			this.getOrCreateTagBuilder(WilderBlockTags.SCULK_SLAB_REPLACEABLE_WORLDGEN)
				.add(Blocks.COBBLED_DEEPSLATE_SLAB)
				.add(Blocks.POLISHED_DEEPSLATE_SLAB)
				.add(Blocks.DEEPSLATE_BRICK_SLAB)
				.add(Blocks.DEEPSLATE_TILE_SLAB)
				.addOptionalTag(WilderBlockTags.SCULK_SLAB_REPLACEABLE);

			this.getOrCreateTagBuilder(WilderBlockTags.SCULK_STAIR_REPLACEABLE)
				.add(Blocks.STONE_STAIRS)
				.add(Blocks.GRANITE_STAIRS)
				.add(Blocks.DIORITE_STAIRS)
				.add(Blocks.ANDESITE_STAIRS)
				.add(Blocks.BLACKSTONE_STAIRS);

			this.getOrCreateTagBuilder(WilderBlockTags.SCULK_STAIR_REPLACEABLE_WORLDGEN)
				.add(Blocks.COBBLED_DEEPSLATE_STAIRS)
				.add(Blocks.POLISHED_DEEPSLATE_STAIRS)
				.add(Blocks.DEEPSLATE_BRICK_STAIRS)
				.add(Blocks.DEEPSLATE_TILE_STAIRS)
				.addOptionalTag(WilderBlockTags.SCULK_STAIR_REPLACEABLE);

			this.getOrCreateTagBuilder(WilderBlockTags.SCULK_WALL_REPLACEABLE)
				.add(Blocks.COBBLESTONE_WALL)
				.add(Blocks.GRANITE_WALL)
				.add(Blocks.DIORITE_WALL)
				.add(Blocks.ANDESITE_WALL)
				.add(Blocks.BLACKSTONE_WALL);

			this.getOrCreateTagBuilder(WilderBlockTags.SCULK_WALL_REPLACEABLE_WORLDGEN)
				.add(Blocks.COBBLED_DEEPSLATE_WALL)
				.add(Blocks.POLISHED_DEEPSLATE_WALL)
				.add(Blocks.DEEPSLATE_BRICK_WALL)
				.add(Blocks.DEEPSLATE_TILE_WALL)
				.addOptionalTag(WilderBlockTags.SCULK_WALL_REPLACEABLE);
		}

		private void generateHollowedAndTermites() {
			this.getOrCreateTagBuilder(WilderBlockTags.BLOCKS_TERMITE)
				.addOptionalTag(ConventionalBlockTags.GLASS_BLOCKS)
				.addOptionalTag(ConventionalBlockTags.GLASS_PANES)
				.add(RegisterBlocks.ECHO_GLASS);

			this.getOrCreateTagBuilder(ConventionalBlockTags.GLASS_BLOCKS)
				.add(RegisterBlocks.ECHO_GLASS);

			this.getOrCreateTagBuilder(WilderBlockTags.BUSH_MAY_PLACE_ON)
				.addOptionalTag(BlockTags.SAND)
				.addOptionalTag(BlockTags.DIRT)
				.addOptionalTag(BlockTags.DEAD_BUSH_MAY_PLACE_ON);

			this.getOrCreateTagBuilder(WilderBlockTags.KILLS_TERMITE)
				.add(Blocks.WATER)
				.add(Blocks.LAVA)
				.add(Blocks.POWDER_SNOW)
				.add(Blocks.WATER_CAULDRON)
				.add(Blocks.LAVA_CAULDRON)
				.add(Blocks.POWDER_SNOW_CAULDRON)
				.add(Blocks.CRIMSON_ROOTS)
				.add(Blocks.CRIMSON_PLANKS)
				.add(Blocks.WARPED_PLANKS)
				.add(Blocks.WEEPING_VINES)
				.add(Blocks.WEEPING_VINES_PLANT)
				.add(Blocks.TWISTING_VINES)
				.add(Blocks.TWISTING_VINES_PLANT)
				.add(Blocks.CRIMSON_SLAB)
				.add(Blocks.WARPED_SLAB)
				.add(Blocks.CRIMSON_PRESSURE_PLATE)
				.add(Blocks.WARPED_PRESSURE_PLATE)
				.add(Blocks.CRIMSON_FENCE)
				.add(Blocks.WARPED_FENCE)
				.add(Blocks.CRIMSON_TRAPDOOR)
				.add(Blocks.WARPED_TRAPDOOR)
				.add(Blocks.CRIMSON_FENCE_GATE)
				.add(Blocks.WARPED_FENCE_GATE)
				.add(Blocks.CRIMSON_STAIRS)
				.add(Blocks.WARPED_STAIRS)
				.add(Blocks.CRIMSON_BUTTON)
				.add(Blocks.WARPED_BUTTON)
				.add(Blocks.CRIMSON_DOOR)
				.add(Blocks.WARPED_DOOR)
				.add(Blocks.CRIMSON_SIGN)
				.add(Blocks.WARPED_SIGN)
				.add(Blocks.CRIMSON_WALL_SIGN)
				.add(Blocks.WARPED_WALL_SIGN)
				.add(Blocks.CRIMSON_STEM)
				.add(Blocks.WARPED_STEM)
				.add(Blocks.STRIPPED_WARPED_STEM)
				.add(Blocks.STRIPPED_WARPED_HYPHAE)
				.add(Blocks.WARPED_NYLIUM)
				.add(Blocks.WARPED_FUNGUS)
				.add(Blocks.STRIPPED_CRIMSON_STEM)
				.add(Blocks.STRIPPED_CRIMSON_HYPHAE)
				.add(Blocks.CRIMSON_NYLIUM)
				.add(Blocks.CRIMSON_FUNGUS)
				.add(Blocks.REDSTONE_WIRE)
				.add(Blocks.REDSTONE_BLOCK)
				.add(Blocks.REDSTONE_TORCH)
				.add(Blocks.REDSTONE_WALL_TORCH)
				.addOptional(ResourceKey.create(
					Registries.BLOCK,
					WilderSharedConstants.id("hollowed_crimson_stem")
				))
				.addOptional(ResourceKey.create(
					Registries.BLOCK,
					WilderSharedConstants.id("hollowed_warped_stem")
				))
				.addOptional(ResourceKey.create(
					Registries.BLOCK,
					WilderSharedConstants.id("stripped_hollowed_crimson_stem")
				))
				.addOptional(ResourceKey.create(
					Registries.BLOCK,
					WilderSharedConstants.id("stripped_hollowed_warped_stem")
				))
				.addOptionalTag(BlockTags.WARPED_STEMS)
				.addOptionalTag(BlockTags.CRIMSON_STEMS);

			this.getOrCreateTagBuilder(WilderBlockTags.TERMITE_BREAKABLE)
				.addOptionalTag(BlockTags.LEAVES)
				.addOptionalTag(WilderBlockTags.STRIPPED_HOLLOWED_LOGS)
				.add(Blocks.BAMBOO)
				.add(Blocks.DEAD_BUSH)
				.add(Blocks.STRIPPED_ACACIA_WOOD)
				.add(Blocks.STRIPPED_BIRCH_WOOD)
				.add(Blocks.STRIPPED_DARK_OAK_WOOD)
				.add(Blocks.STRIPPED_JUNGLE_WOOD)
				.add(Blocks.STRIPPED_MANGROVE_WOOD)
				.add(Blocks.STRIPPED_OAK_WOOD)
				.add(Blocks.STRIPPED_SPRUCE_WOOD)
				.add(Blocks.STRIPPED_ACACIA_WOOD)
				.addOptional(
					ResourceKey.create(
						Registries.BLOCK,
						WilderSharedConstants.id("stripped_baobab_wood")
					)
				)
				.addOptional(
					ResourceKey.create(
						Registries.BLOCK,
						WilderSharedConstants.id("stripped_cypress_wood")
					)
				)
				.addOptional(
					ResourceKey.create(
						Registries.BLOCK,
						WilderSharedConstants.id("stripped_palm_wood")
					)
				)
				.addOptional(
					ResourceKey.create(
						Registries.BLOCK,
						new ResourceLocation("immersive_weathering", "leaf_piles")
					)
				);

			this.getOrCreateTagBuilder(WilderBlockTags.SAND_POOL_REPLACEABLE)
				.add(Blocks.SAND);
		}
	}

	private static final class WilderDamageTypeTagProvider extends FabricTagProvider<DamageType> {

		public WilderDamageTypeTagProvider(@NotNull FabricDataOutput output, @NotNull CompletableFuture<HolderLookup.Provider> lookupProvider) {
			super(output, Registries.DAMAGE_TYPE, lookupProvider);
		}

		@Override
		public void addTags(@NotNull HolderLookup.Provider arg) {
			this.getOrCreateTagBuilder(DamageTypeTags.NO_ANGER)
				.add(RegisterDamageTypes.TUMBLEWEED)
				.add(RegisterDamageTypes.PRICKLY_PEAR);

			this.getOrCreateTagBuilder(DamageTypeTags.BYPASSES_ARMOR)
				.add(RegisterDamageTypes.ANCIENT_HORN);

			this.getOrCreateTagBuilder(DamageTypeTags.BYPASSES_EFFECTS)
				.add(RegisterDamageTypes.PRICKLY_PEAR);

			this.getOrCreateTagBuilder(DamageTypeTags.BYPASSES_ENCHANTMENTS)
				.add(RegisterDamageTypes.ANCIENT_HORN);

			this.getOrCreateTagBuilder(DamageTypeTags.WITCH_RESISTANT_TO)
				.add(RegisterDamageTypes.ANCIENT_HORN);
		}
	}

	private static final class WilderItemTagProvider extends FabricTagProvider.ItemTagProvider {

		public WilderItemTagProvider(@NotNull FabricDataOutput output, @NotNull CompletableFuture<HolderLookup.Provider> completableFuture) {
			super(output, completableFuture);
		}

		@Override
		protected void addTags(@NotNull HolderLookup.Provider arg) {
			this.getOrCreateTagBuilder(FrozenItemTags.ALWAYS_SAVE_COOLDOWNS)
				.add(RegisterItems.ANCIENT_HORN);

			this.getOrCreateTagBuilder(WilderItemTags.BROWN_MUSHROOM_STEW_INGREDIENTS)
				.add(Items.BROWN_MUSHROOM)
				.addOptional(WilderSharedConstants.id("brown_shelf_fungus"));

			this.getOrCreateTagBuilder(WilderItemTags.RED_MUSHROOM_STEW_INGREDIENTS)
				.add(Items.RED_MUSHROOM)
				.addOptional(WilderSharedConstants.id("red_shelf_fungus"));
		}
	}

	private static final class WilderEntityTagProvider extends FabricTagProvider.EntityTypeTagProvider {

		public WilderEntityTagProvider(@NotNull FabricDataOutput output, @NotNull CompletableFuture<HolderLookup.Provider> completableFuture) {
			super(output, completableFuture);
		}

		@Override
		protected void addTags(@NotNull HolderLookup.Provider arg) {
			this.getOrCreateTagBuilder(WilderEntityTags.STAYS_IN_MESOGLEA)
				.add(RegisterEntities.JELLYFISH);
		}
	}

	private static class ExperimentBlockLootTableProvider extends FabricBlockLootTableProvider {
		protected ExperimentBlockLootTableProvider(@NotNull FabricDataOutput dataOutput) {
			super(dataOutput);
		}

		@Override
		public void generate() {
			this.dropSelf(RegisterBlocks.BAOBAB_HANGING_SIGN);
			this.dropSelf(RegisterBlocks.CYPRESS_HANGING_SIGN);
		}
	}

	private static class ExperimentBlockTagProvider extends FabricTagProvider.BlockTagProvider {

		public ExperimentBlockTagProvider(@NotNull FabricDataOutput output, @NotNull CompletableFuture<HolderLookup.Provider> registriesFuture) {
			super(output, registriesFuture);
		}

		private static ResourceKey<Block> key(@NotNull Block block) {
			return BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow();
		}

		@Override
		protected void addTags(@NotNull HolderLookup.Provider arg) {
			this.tag(BlockTags.CEILING_HANGING_SIGNS)
				.add(key(RegisterBlocks.BAOBAB_HANGING_SIGN))
				.add(key(RegisterBlocks.CYPRESS_HANGING_SIGN));

			this.tag(BlockTags.WALL_HANGING_SIGNS)
				.add(key(RegisterBlocks.BAOBAB_WALL_HANGING_SIGN))
				.add(key(RegisterBlocks.CYPRESS_WALL_HANGING_SIGN));
		}
	}

	private static class ExperimentRecipeProvider extends RecipeProvider {

		public ExperimentRecipeProvider(@NotNull PackOutput packOutput) {
			super(packOutput);
		}

		@Override
		public void buildRecipes(final @NotNull Consumer<FinishedRecipe> consumer) {
			generateForEnabledBlockFamilies(consumer, FeatureFlagSet.of(WilderFeatureFlags.UPDATE_1_20_ADDITIONS));
			hangingSign(consumer, RegisterItems.BAOBAB_HANGING_SIGN, RegisterBlocks.STRIPPED_BAOBAB_LOG);
			hangingSign(consumer, RegisterItems.CYPRESS_HANGING_SIGN, RegisterBlocks.STRIPPED_CYPRESS_LOG);
		}
	}
}
