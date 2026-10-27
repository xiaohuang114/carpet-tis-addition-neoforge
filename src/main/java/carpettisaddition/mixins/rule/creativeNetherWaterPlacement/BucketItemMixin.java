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

package carpettisaddition.mixins.rule.creativeNetherWaterPlacement;

import carpettisaddition.CarpetTISAdditionSettings;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin
{
	@ModifyExpressionValue(
			// S4 / B-class: 1.21.1 has three emptyContents overloads, the bare name is ambiguous (0 targets).
			// DimensionType.ultraWarm() is invoked at offset 228 of the 5-arg one (javap -c BucketItem).
			method = "emptyContents(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/item/ItemStack;)Z",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/dimension/DimensionType;ultraWarm()Z"
			)
	)
	private boolean creativeNetherWaterPlacement_impl(boolean doesWaterVaporize,
			@Local(argsOnly = true) @Nullable Player player
	)
	{
		if (CarpetTISAdditionSettings.creativeNetherWaterPlacement)
		{
			if (player != null && player.isCreative())
			{
				doesWaterVaporize = false;
			}
		}
		return doesWaterVaporize;
	}
}
