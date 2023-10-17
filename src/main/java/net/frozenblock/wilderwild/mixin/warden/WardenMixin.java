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

package net.frozenblock.wilderwild.mixin.warden;

import net.frozenblock.wilderwild.config.EntityConfig;
import net.frozenblock.wilderwild.entity.Tumbleweed;
import net.frozenblock.wilderwild.entity.render.animations.WilderWarden;
import net.frozenblock.wilderwild.misc.interfaces.SwimmingWardenInterface;
import net.frozenblock.wilderwild.registry.RegisterProperties;
import net.frozenblock.wilderwild.registry.RegisterSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Unit;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value = Warden.class, priority = 69420)
public final class WardenMixin extends Monster implements WilderWarden {

	@Unique
	private final AnimationState wilderWild$dyingAnimationState = new AnimationState();
	@Unique
	private final AnimationState wilderWild$swimmingDyingAnimationState = new AnimationState();
	@Unique
	private final AnimationState wilderWild$kirbyDeathAnimationState = new AnimationState();
	@Unique
	private int wilderWild$deathTicks = 0;

	private WardenMixin(EntityType<? extends Monster> entityType, Level level) {
		super(entityType, level);
	}

	@Shadow
	public Brain<Warden> getBrain() {
		throw new AssertionError("Mixin injection failed - Wilder Wild WardenMixin.");
	}

	@Shadow
	private void clientDiggingParticles(AnimationState animationState) {
		throw new AssertionError("Mixin injection failed - Wilder Wild WardenMixin.");
	}

	@Shadow
	public boolean isDiggingOrEmerging() {
		throw new AssertionError("Mixin injection failed - Wilder Wild WardenMixin.");
	}

	@Unique
	@Override
	public AnimationState wilderWild$getDyingAnimationState() {
		return this.wilderWild$dyingAnimationState;
	}

	@Unique
	@Override
	public AnimationState wilderWild$getSwimmingDyingAnimationState() {
		return this.wilderWild$swimmingDyingAnimationState;
	}

	@Unique
	@Override
	public AnimationState wilderWild$getKirbyDeathAnimationState() {
		return this.wilderWild$kirbyDeathAnimationState;
	}

	@Unique
	@Override
	public boolean wilderWild$isStella() {
		Warden warden = Warden.class.cast(this);
		String name = ChatFormatting.stripFormatting(warden.getName().getString());
		return name != null && (name.equalsIgnoreCase("Stella") || name.equalsIgnoreCase("Osmiooo") || name.equalsIgnoreCase("Mossmio") || name.equalsIgnoreCase("Osmio"));
	}

	@Inject(at = @At("RETURN"), method = "getDeathSound")
	public void wilderWild$getDeathSound(CallbackInfoReturnable<SoundEvent> info) {
		Warden warden = Warden.class.cast(this);
		if (this.wilderWild$isStella()) {
			warden.playSound(RegisterSounds.ENTITY_WARDEN_KIRBY_DEATH, 5.0F, 1.0F);
		} else {
			if (EntityConfig.get().warden.wardenDyingAnimation) {
				if (warden instanceof SwimmingWardenInterface swim && swim.wilderWild$isSubmergedInWaterOrLava()) {
					warden.playSound(RegisterSounds.ENTITY_WARDEN_UNDERWATER_DYING, 0.75F, 1.0F);
				} else {
					warden.playSound(RegisterSounds.ENTITY_WARDEN_DYING, 5.0F, 1.0F);
				}
			}
		}
	}

	@Inject(at = @At("RETURN"), method = "finalizeSpawn")
	public void wilderWild$finalizeSpawn(ServerLevelAccessor serverLevelAccess, DifficultyInstance localDifficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData entityData, @Nullable CompoundTag nbtCompound, CallbackInfoReturnable<SpawnGroupData> info) {
		if ((EntityConfig.get().warden.wardenEmergesFromEgg && spawnReason == MobSpawnType.SPAWN_EGG) || (EntityConfig.get().warden.wardenEmergesFromCommand && spawnReason == MobSpawnType.COMMAND)) {
			this.setPose(Pose.EMERGING);
			this.getBrain().setMemoryWithExpiry(MemoryModuleType.IS_EMERGING, Unit.INSTANCE, WardenAi.EMERGE_DURATION);
			this.playSound(SoundEvents.WARDEN_AGITATED, 5.0f, 1.0f);
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/Brain;setMemoryWithExpiry(Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;Ljava/lang/Object;J)V", shift = At.Shift.BEFORE, ordinal = 0), method = "doPush")
	private void wilderWild$doPush(Entity entity, CallbackInfo info) {
		Warden warden = Warden.class.cast(this);
		if (!warden.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_COOLING_DOWN)
			&& !(entity instanceof Warden)
			&& entity instanceof LivingEntity livingEntity
			&& !entity.isInvulnerable()
			&& !warden.isDiggingOrEmerging()
			&& !warden.hasPose(Pose.DYING)
			&& !warden.hasPose(Pose.ROARING)
			&& EntityConfig.get().warden.wardenAttacksImmediately
		) {
			if (!(entity instanceof Player player) || !player.isCreative()) {
				warden.increaseAngerAt(entity, AngerLevel.ANGRY.getMinimumAnger() + 20, false);
				if (!livingEntity.isDeadOrDying() && warden.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).isEmpty()) {
					warden.setAttackTarget(livingEntity);
				}
			}
		}
	}

	@Inject(at = @At("HEAD"), method = "canTargetEntity(Lnet/minecraft/world/entity/Entity;)Z", cancellable = true)
	public void canTargetEntity(Entity entity, CallbackInfoReturnable<Boolean> info) {
		if (entity instanceof Tumbleweed) {
			info.setReturnValue(false);
		}
	}

	@Inject(method = "onSyncedDataUpdated", at = @At("HEAD"), cancellable = true)
	private void wilderWild$onSyncedDataUpdated(EntityDataAccessor<?> data, CallbackInfo info) {
		Warden warden = Warden.class.cast(this);
		if (EntityConfig.get().warden.wardenDyingAnimation || this.wilderWild$isStella()) {
			if (DATA_POSE.equals(data)) {
				if (warden.getPose() == Pose.DYING) {
					if (this.wilderWild$isStella()) {
						this.wilderWild$getKirbyDeathAnimationState().start(warden.tickCount);
					} else {
						if (warden instanceof SwimmingWardenInterface swim && swim.wilderWild$isSubmergedInWaterOrLava()) {
							this.wilderWild$getSwimmingDyingAnimationState().start(warden.tickCount);
						} else {
							this.wilderWild$getDyingAnimationState().start(warden.tickCount);
						}
					}
					info.cancel();
				}
			}
		}
	}

	@Unique
	private void wilderWild$addAdditionalDeathParticles() {
		for (int i = 0; i < 20; ++i) {
			double d = this.random.nextGaussian() * 0.02;
			double e = this.random.nextGaussian() * 0.02;
			double f = this.random.nextGaussian() * 0.02;
			this.level().addParticle(ParticleTypes.SCULK_CHARGE_POP, this.getRandomX(1.0), this.getRandomY(), this.getRandomZ(1.0), d, e, f);
			this.level().addParticle(ParticleTypes.SCULK_SOUL, this.getRandomX(1.0), this.getRandomY(), this.getRandomZ(1.0), d, e, f);
		}
	}

	@Unique
	@Override
	public int wilderWild$getDeathTicks() {
		return this.wilderWild$deathTicks;
	}

	@Unique
	@Override
	public void wilderWild$setDeathTicks(int i) {
		this.wilderWild$deathTicks = i;
	}

	@ModifyArgs(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V", ordinal = 0))
	private void wilderWild$stellaHeartbeat(Args args) {
		if (this.wilderWild$isStella()) {
			args.set(3, RegisterSounds.ENTITY_WARDEN_STELLA_HEARTBEAT);
		}
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void wilderWild$tick(CallbackInfo info) {
		Warden warden = Warden.class.cast(this);
		if (EntityConfig.get().warden.wardenDyingAnimation || this.wilderWild$isStella()) {
			if (warden.getPose() == Pose.DYING) {
				this.clientDiggingParticles(this.wilderWild$getDyingAnimationState());
			}
		}
		if ((warden.isInWaterOrBubble() || warden.isInLava())
			&& (!warden.isEyeInFluid(FluidTags.WATER) || !warden.isEyeInFluid(FluidTags.LAVA))
			&& this.horizontalCollision
			&& !this.isDiggingOrEmerging()
			&& this.navigation.isInProgress()
			&& this.navigation.getTargetPos() != null
			&& this.navigation.getTargetPos().getY() > this.getBlockY()
		) {
			this.setDeltaMovement(this.getDeltaMovement().add(0, 0.05, 0));
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	public void wilderWild$addAdditionalSaveData(CompoundTag nbt, CallbackInfo info) {
		nbt.putInt("wilderDeathTicks", this.wilderWild$deathTicks);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	public void wilderWild$readAdditionalSaveData(CompoundTag nbt, CallbackInfo info) {
		this.wilderWild$deathTicks = nbt.getInt("wilderDeathTicks");
	}

	@Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
	private void wilderWild$handleEntityEvent(byte status, CallbackInfo info) {
		if (status == (byte) 69420) {
			this.wilderWild$addAdditionalDeathParticles();
			info.cancel();
		}
	}

	@Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
	public void wilderWild$getDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> info) {
		if (!this.isDiggingOrEmerging()) {
			if (EntityConfig.get().warden.wardenDyingAnimation || this.wilderWild$isStella()) {
				if (wilderWild$deathTicks > 0) {
					info.setReturnValue(EntityDimensions.fixed(this.getType().getWidth(), 0.35F));
				}
			}
		}
	}

	@Mixin(Warden.VibrationUser.class)
	public static class VibrationUserMixin {

		@Shadow
		@Final
		Warden field_44600;

		@Inject(method = "onReceiveVibration", at = @At("HEAD"))
		private void wilderWild$onReceiveVibration(ServerLevel world, BlockPos pos, GameEvent event, Entity sourceEntity, Entity entity, float distance, CallbackInfo ci) {
			if (!this.field_44600.isDeadOrDying()) {
				int additionalAnger = 0;
				if (this.field_44600.level().getBlockState(pos).is(Blocks.SCULK_SENSOR)) {
					if (this.field_44600.level().getBlockState(pos).getValue(RegisterProperties.HICCUPPING)) {
						additionalAnger = 65;
					}
				}
				if (sourceEntity != null) {
					if (this.field_44600.closerThan(sourceEntity, 30.0D)) {
						this.field_44600.increaseAngerAt(sourceEntity, additionalAnger, false);
					}
				} else {
					this.field_44600.increaseAngerAt(entity, additionalAnger, false);
				}
			}
		}
	}
}
