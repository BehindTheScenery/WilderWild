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

package net.frozenblock.wilderwild.mixin.projectile;

import net.frozenblock.wilderwild.config.ItemConfig;
import net.frozenblock.wilderwild.registry.RegisterSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Snowball.class)
public class SnowballMixin {

	@Inject(method = "onHit", at = @At("HEAD"))
	public void wilderWild$onHit(HitResult result, CallbackInfo info) {
		if (ItemConfig.get().projectileLandingSounds.snowballLandingSounds) {
			Snowball snowball = Snowball.class.cast(this);
			snowball.level().playSound(null, snowball.getX(), snowball.getY(), snowball.getZ(), RegisterSounds.ITEM_SNOWBALL_LAND, SoundSource.BLOCKS, 0.3F, 0.85F + (snowball.level().random.nextFloat() * 0.2F));
		}
	}

}
