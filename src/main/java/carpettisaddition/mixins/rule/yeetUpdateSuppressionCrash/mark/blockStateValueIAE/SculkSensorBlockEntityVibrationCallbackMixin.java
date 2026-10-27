/*
 * This file is part of the Carpet TIS Addition project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2024  Fallen_Breath and contributors
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

package carpettisaddition.mixins.rule.yeetUpdateSuppressionCrash.mark.blockStateValueIAE;

import carpettisaddition.CarpetTISAdditionSettings;
import carpettisaddition.helpers.rule.yeetUpdateSuppressionCrash.UpdateSuppressionException;
import carpettisaddition.helpers.rule.yeetUpdateSuppressionCrash.UpdateSuppressionYeeter;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.server.level.ServerLevel;

@Mixin(
		SculkSensorBlockEntity.VibrationUser.class
)
public abstract class SculkSensorBlockEntityVibrationCallbackMixin
{
	@Shadow @Final protected BlockPos blockPos;

	@WrapOperation(
			method = {"canReceiveVibration", "onReceiveVibration"},
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/SculkSensorBlock;canActivate(Lnet/minecraft/world/level/block/state/BlockState;)Z"
			)
	)
	private boolean yeetUpdateSuppressionCrash_wrapSoundSuppression_sculkSensor(
			BlockState instance, Operation<Boolean> original,
			@Local(argsOnly = true) ServerLevel world
	) throws Throwable
	{
		if (CarpetTISAdditionSettings.yeetUpdateSuppressionCrash)
		{
			try
			{
				return original.call(instance);
			}
			catch (Throwable throwable)
			{
				if (throwable instanceof UpdateSuppressionException || throwable instanceof IllegalArgumentException)
				{
					BlockPos pos = this.blockPos;
					throw UpdateSuppressionYeeter.tryReplaceWithWrapper(throwable, world, pos);
				}
				else
				{
					throw throwable;
				}
			}
		}
		else
		{
			// vanilla
			return original.call(instance);
		}
	}
}
