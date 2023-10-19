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

package net.frozenblock.wilderwild.config.gui;

import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.lib.config.clothconfig.FrozenClothConfig;
import net.frozenblock.wilderwild.config.ItemConfig;
import net.frozenblock.wilderwild.misc.WilderSharedConstants;
import static net.frozenblock.wilderwild.misc.WilderSharedConstants.text;
import static net.frozenblock.wilderwild.misc.WilderSharedConstants.tooltip;

@Environment(EnvType.CLIENT)
public final class ItemConfigGui {
	private ItemConfigGui() {
		throw new UnsupportedOperationException("ItemConfigGui contains only static declarations.");
	}

	public static void setupEntries(ConfigCategory category, ConfigEntryBuilder entryBuilder) {
		var config = ItemConfig.get(true);
		var defaultConfig = ItemConfig.INSTANCE.defaultInstance();
		var ancientHorn = config.ancientHorn;
		var projectileLandingSounds = config.projectileLandingSounds;
		category.setBackground(WilderSharedConstants.id("textures/config/item.png"));
		var summonsWarden = entryBuilder.startBooleanToggle(text("ancient_horn_can_summon_warden"), ancientHorn.ancientHornCanSummonWarden)
			.setDefaultValue(defaultConfig.ancientHorn.ancientHornCanSummonWarden)
			.setSaveConsumer(newValue -> ancientHorn.ancientHornCanSummonWarden = newValue)
			.setTooltip(tooltip("ancient_horn_can_summon_warden"))
			.build();

		var lifespan = entryBuilder.startIntSlider(text("ancient_horn_lifespan"), ancientHorn.ancientHornLifespan, 0, 1000)
			.setDefaultValue(defaultConfig.ancientHorn.ancientHornLifespan)
			.setSaveConsumer(newValue -> ancientHorn.ancientHornLifespan = newValue)
			.setTooltip(tooltip("ancient_horn_lifespan"))
			.build();

		var mobDamage = entryBuilder.startIntSlider(text("ancient_horn_mob_damage"), ancientHorn.ancientHornMobDamage, 0, 50)
			.setDefaultValue(defaultConfig.ancientHorn.ancientHornMobDamage)
			.setSaveConsumer(newValue -> ancientHorn.ancientHornMobDamage = newValue)
			.setTooltip(tooltip("ancient_horn_mob_damage"))
			.build();

		var playerDamage = entryBuilder.startIntSlider(text("ancient_horn_player_damage"), ancientHorn.ancientHornPlayerDamage, 0, 50)
			.setDefaultValue(defaultConfig.ancientHorn.ancientHornPlayerDamage)
			.setSaveConsumer(newValue -> ancientHorn.ancientHornPlayerDamage = newValue)
			.setTooltip(tooltip("ancient_horn_player_damage"))
			.build();

		var shattersGlass = entryBuilder.startBooleanToggle(text("ancient_horn_shatters_glass"), ancientHorn.ancientHornShattersGlass)
			.setDefaultValue(defaultConfig.ancientHorn.ancientHornShattersGlass)
			.setSaveConsumer(newValue -> ancientHorn.ancientHornShattersGlass = newValue)
			.setTooltip(tooltip("ancient_horn_shatters_glass"))
			.build();

		var sizeMultiplier = entryBuilder.startFloatField(text("ancient_horn_size_multiplier"), ancientHorn.ancientHornSizeMultiplier)
			.setDefaultValue(defaultConfig.ancientHorn.ancientHornSizeMultiplier)
			.setSaveConsumer(newValue -> ancientHorn.ancientHornSizeMultiplier = newValue)
			.setTooltip(tooltip("ancient_horn_size_multiplier"))
			.build();

		var ancientHornCategory = FrozenClothConfig.createSubCategory(entryBuilder, category, text("ancient_horn"),
			false,
			tooltip("ancient_horn"),
			summonsWarden, lifespan, mobDamage, playerDamage, shattersGlass, sizeMultiplier
		);

		var snowballLandingSounds = entryBuilder.startBooleanToggle(text("snowball_landing_sounds"), projectileLandingSounds.snowballLandingSounds)
			.setDefaultValue(defaultConfig.projectileLandingSounds.snowballLandingSounds)
			.setSaveConsumer(newValue -> projectileLandingSounds.snowballLandingSounds = newValue)
			.setTooltip(tooltip("snowball_landing_sounds"))
			.build();

		var eggLandingSounds = entryBuilder.startBooleanToggle(text("egg_landing_sounds"), projectileLandingSounds.eggLandingSounds)
			.setDefaultValue(defaultConfig.projectileLandingSounds.eggLandingSounds)
			.setSaveConsumer(newValue -> projectileLandingSounds.eggLandingSounds = newValue)
			.setTooltip(tooltip("egg_landing_sounds"))
			.build();

		var enderPearlLandingSounds = entryBuilder.startBooleanToggle(text("ender_pearl_landing_sounds"), projectileLandingSounds.enderPearlLandingSounds)
			.setDefaultValue(defaultConfig.projectileLandingSounds.enderPearlLandingSounds)
			.setSaveConsumer(newValue -> projectileLandingSounds.enderPearlLandingSounds = newValue)
			.setTooltip(tooltip("ender_pearl_landing_sounds"))
			.build();

		var potionLandingSounds = entryBuilder.startBooleanToggle(text("potion_landing_sounds"), projectileLandingSounds.potionLandingSounds)
			.setDefaultValue(defaultConfig.projectileLandingSounds.potionLandingSounds)
			.setSaveConsumer(newValue -> projectileLandingSounds.potionLandingSounds = newValue)
			.setTooltip(tooltip("potion_landing_sounds"))
			.build();

		var projectileLandingSoundsCategory = FrozenClothConfig.createSubCategory(entryBuilder, category, text("projectile_landing_sounds"),
			false,
			tooltip("projectile_landing_sounds"),
			snowballLandingSounds, eggLandingSounds, enderPearlLandingSounds, potionLandingSounds
		);

		var projectileBreakParticles = category.addEntry(entryBuilder.startBooleanToggle(text("projectile_break_particles"), config.projectileBreakParticles)
			.setDefaultValue(defaultConfig.projectileBreakParticles)
			.setSaveConsumer(newValue -> config.projectileBreakParticles = newValue)
			.setTooltip(tooltip("projectile_break_particles"))
			.build()
		);

		var restrictInstrumentSound = category.addEntry(entryBuilder.startBooleanToggle(text("restrict_instrument_sound"), config.restrictInstrumentSound)
			.setDefaultValue(defaultConfig.restrictInstrumentSound)
			.setSaveConsumer(newValue -> config.restrictInstrumentSound = newValue)
			.setTooltip(tooltip("restrict_instrument_sound"))
			.build()
		);
	}
}
