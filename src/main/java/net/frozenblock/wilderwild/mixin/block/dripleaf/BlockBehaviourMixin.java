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

package net.frozenblock.wilderwild.mixin.block.dripleaf;

import net.frozenblock.wilderwild.entity.Tumbleweed;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BigDripleafStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {

	@Inject(at = @At("HEAD"), method = "neighborChanged", cancellable = true)
	public void wilderWild$neighborChanged(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving, CallbackInfo info) {
		if (BlockBehaviour.class.cast(this) instanceof BigDripleafStemBlock && !level.isClientSide) {
			BlockState downState = level.getBlockState(pos.below());
			boolean receivingPower = level.hasNeighborSignal(pos) || (downState.is(Blocks.BIG_DRIPLEAF_STEM) && downState.getValue(BlockStateProperties.POWERED));
			if (state.getValue(BlockStateProperties.POWERED) != receivingPower) {
				level.setBlock(pos, state.setValue(BlockStateProperties.POWERED, receivingPower), 3);
			}
			info.cancel();
		}
	}

	@Inject(at = @At("HEAD"), method = "getCollisionShape", cancellable = true)
	public void wilderWild$getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> info) {
		if (BlockBehaviour.class.cast(this) instanceof LeavesBlock && context instanceof EntityCollisionContext entityCollisionContext && entityCollisionContext.getEntity() instanceof Tumbleweed tumbleweed) {
			tumbleweed.isTouchingStickingBlock = true;
			info.setReturnValue(Shapes.empty());
		}
	}

}
