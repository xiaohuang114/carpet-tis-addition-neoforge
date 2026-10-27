/*
 * This file is part of the Carpet TIS Addition project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2025  Fallen_Breath and contributors
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

package carpettisaddition.mixins.logger.portalCreation;

import carpettisaddition.logging.TISAdditionLoggerRegistry;
import carpettisaddition.logging.loggers.portalCreation.PortalCreationLogger;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.portal.PortalForcer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import net.minecraft.core.Vec3i;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(PortalForcer.class)
public abstract class PortalForcerMixin
{
	@Shadow
	@Final
	private ServerLevel level;

	@Inject(
			method = "createPortal",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/util/Mth;clamp(III)I"
			)
	)
	private void portalCreationLogger_recordIsFloatingPlatform(CallbackInfoReturnable<Boolean> cir, @Share("isFloatingPlatform") LocalBooleanRef isFloatingPlatform)
	{
		isFloatingPlatform.set(true);
	}
	@ModifyArg(
			method = "createPortal",
			slice = @Slice(
					from = @At(
							value = "FIELD",
							target = "Lnet/minecraft/world/level/block/Blocks;NETHER_PORTAL:Lnet/minecraft/world/level/block/Block;"
					)
			),
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/core/BlockPos$MutableBlockPos;setWithOffset(Lnet/minecraft/core/Vec3i;III)Lnet/minecraft/core/BlockPos$MutableBlockPos;",
					ordinal = 0
			)
	)
	private
	Vec3i
	portalCreationLogger_doLog(
			Vec3i arg0,
			@Share("isNotFirstSet") LocalBooleanRef isNotFirstSet,
			@Share("isFloatingPlatform") LocalBooleanRef isFloatingPlatform
	)
	{
		if (!TISAdditionLoggerRegistry.__portalCreation || isNotFirstSet.get())
		{
			return arg0;
		}
		isNotFirstSet.set(true);
		{
			BlockPos portalPos = new BlockPos(arg0.getX(), arg0.getY(), arg0.getZ());
			PortalCreationLogger.getInstance().onNetherPortalCreation(this.level, portalPos, isFloatingPlatform.get());
		}
		return arg0;
	}
}
