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

package net.frozenblock.wilderwild.mixin.client.mesoglea;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.wilderwild.block.MesogleaBlock;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

	@Unique
	private static BlockState wilderWild$currentBlockState;

	@ModifyExpressionValue(method = "renderBlockShadow", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"), require = 0)
	private static BlockState wilderWild$captureBlockState(BlockState original) {
		wilderWild$currentBlockState = original;
		return original;
	}

	@Inject(method = "renderBlockShadow", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LightTexture;getBrightness(Lnet/minecraft/world/level/dimension/DimensionType;I)F", shift = At.Shift.BEFORE), cancellable = true, require = 0)
	private static void wilderWild$stopRenderIfMesoglea(PoseStack.Pose pose, VertexConsumer vertexConsumer, ChunkAccess chunkAccess, LevelReader levelReader, BlockPos blockPos, double d, double e, double f, float g, float h, CallbackInfo info) {
		if (wilderWild$currentBlockState.getBlock() instanceof MesogleaBlock && (wilderWild$currentBlockState.hasProperty(BlockStateProperties.WATERLOGGED) && wilderWild$currentBlockState.getValue(BlockStateProperties.WATERLOGGED))) {
			info.cancel();
		}
		wilderWild$currentBlockState = null;
	}

}
