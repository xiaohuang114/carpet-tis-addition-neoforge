/*
 * This file is part of the Carpet TIS Addition project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2023  Fallen_Breath and contributors
 *
 * Carpet TIS Addition is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Carpet TIS Addition is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Carpet TIS Addition.  If not, see <https://www.gnu.org/licenses/>.
 */

package carpettisaddition.mixins.logger.raid;

import carpettisaddition.commands.raid.RaidTracker;
import carpettisaddition.logging.loggers.raid.IRaid;
import carpettisaddition.logging.loggers.raid.RaidLogger;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Raid.class)
public abstract class RaidMixin implements IRaid
{
	@Shadow private int raidOmenLevel;

	@Shadow public abstract boolean isVictory();
	@Inject(method = "<init>(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V", at = @At("TAIL"))
	private void raidLogger_onConstruct(CallbackInfo ci)
	{
		RaidLogger.getInstance().onRaidCreated((Raid)(Object)this);
	}

	@Override
	public void onRaidInvalidated$TISCM(RaidLogger.InvalidateReason reason)
	{
		RaidLogger.getInstance().onRaidInvalidated((Raid)(Object)this, reason);
		RaidTracker.getInstance().trackRaidInvalidated(reason);
	}

	@Inject(
			method = "absorbRaidOmen",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/server/level/ServerPlayer;getEffect(Lnet/minecraft/core/Holder;)Lnet/minecraft/world/effect/MobEffectInstance;"
			)
	)
	private void raidLogger_onStartBeforeCalculated(
			CallbackInfoReturnable<Boolean> cir,
			@Share("previousBadOmenLevel") LocalIntRef previousBadOmenLevel
	)
	{
		previousBadOmenLevel.set(this.raidOmenLevel);
	}

	@Inject(
			method = "absorbRaidOmen",
			at = @At(
					value = "INVOKE_ASSIGN",
					shift = At.Shift.AFTER,
					target = "Lnet/minecraft/util/Mth;clamp(III)I"
			)
	)
	private void raidLogger_onStartedAfterCalculated(
			CallbackInfoReturnable<Boolean> cir,
			@Share("previousBadOmenLevel") LocalIntRef previousBadOmenLevel
	)
	{
		if (this.raidOmenLevel > 1 && this.raidOmenLevel > previousBadOmenLevel.get())
		{
			RaidLogger.getInstance().onBadOmenLevelIncreased((Raid)(Object)this, this.raidOmenLevel);
		}
	}

	/*
	 * -------------------------
	 *    onInvalidated start
	 * -------------------------
	 */

	@Inject(
			method = "tick",
			slice = @Slice(
					from = @At(
							value = "INVOKE",
							target = "Lnet/minecraft/server/level/ServerLevel;getDifficulty()Lnet/minecraft/world/Difficulty;"
					)
			),
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/raid/Raid;stop()V",
					ordinal = 0
			)
	)
	private void raidLogger_onInvalidatedByDifficulty(CallbackInfo ci)
	{
		onRaidInvalidated$TISCM(RaidLogger.InvalidateReason.DIFFICULTY_PEACEFUL);
	}

	@Inject(
			method = "tick",
			slice = @Slice(
					from = @At(
							value = "INVOKE",
							target = "Lnet/minecraft/server/level/ServerLevel;isVillage(Lnet/minecraft/core/BlockPos;)Z"
					)
			),
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/raid/Raid;stop()V",
					ordinal = 0
			)
	)
	private void raidLogger_onInvalidatedByPOINotFound(CallbackInfo ci)
	{
		onRaidInvalidated$TISCM(RaidLogger.InvalidateReason.POI_REMOVED_BEFORE_SPAWN);
	}

	@Inject(
			method = "tick",
			slice = @Slice(
					from = @At(
							value = "CONSTANT",
							args = "longValue=48000",
							ordinal = 0
					)
			),
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/raid/Raid;stop()V",
					ordinal = 0
			)
	)
	private void raidLogger_onInvalidatedByTimeOut(CallbackInfo ci)
	{
		onRaidInvalidated$TISCM(RaidLogger.InvalidateReason.TIME_OUT);
	}

	@Inject(
			method = "tick",
			slice = @Slice(
					from = @At(
							value = "INVOKE",
							target = "Lnet/minecraft/world/entity/raid/Raid;playSound(Lnet/minecraft/core/BlockPos;)V"
					)
			),
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/raid/Raid;stop()V",
					ordinal = 0
			)
	)
	private void raidLogger_onInvalidatedByRaiderCannotSpawn(CallbackInfo ci)
	{
		onRaidInvalidated$TISCM(RaidLogger.InvalidateReason.RAIDER_CANNOT_SPAWN);
	}

	@Inject(
			method = "tick",
			slice = @Slice(
					from = @At(
							value = "CONSTANT",
							args = "intValue=600"
					)
			),
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/raid/Raid;stop()V",
					ordinal = 0
			)
	)
	private void raidLogger_onInvalidatedByFinished(CallbackInfo ci)
	{
		if (this.isVictory())
		{
			onRaidInvalidated$TISCM(RaidLogger.InvalidateReason.RAID_VICTORY);
		}
		else
		{
			onRaidInvalidated$TISCM(RaidLogger.InvalidateReason.RAID_DEFEAT);
		}
	}

	/*
	 * -----------------------
	 *    onInvalidated end
	 * -----------------------
	 */

	@Inject(
			method = "setCenter",
			at = @At(value = "HEAD")
	)
	private void raidLogger_onCenterMoved(BlockPos blockPos, CallbackInfo ci)
	{
		RaidLogger.getInstance().onCenterMoved((Raid)(Object)this, blockPos);
	}
}
