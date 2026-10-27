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

package carpettisaddition.mixins.rule.netherPortalMaxSize;

import carpettisaddition.CarpetTISAdditionSettings;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.world.level.portal.PortalShape;

@Mixin(
		PortalShape.class
)
public abstract class NetherPortalBlockAreaHelperMixin
{
	@ModifyExpressionValue(
			method = {
					"calculateWidth",
					"getDistanceUntilEdgeAboveFrame",
					"calculateBottomLeft",
					"calculateHeight",
					"getDistanceUntilTop",
			},
			at = @At(
					value = "CONSTANT",
					args = "intValue=21"
			)
	)
	private
	int netherPortalMaxSize_modifyUpperLimit1(int limit)
	{
		if (CarpetTISAdditionSettings.netherPortalMaxSize != CarpetTISAdditionSettings.VANILLA_NETHER_PORTAL_MAX_SIZE)
		{
			limit = CarpetTISAdditionSettings.netherPortalMaxSize;
		}
		return limit;
	}

	@ModifyExpressionValue(
			method = "isValid",
			at = @At(
					value = "CONSTANT",
					args = "intValue=21"
			)
	)
	private int netherPortalMaxSize_modifyUpperLimit2(int limit)
	{
		if (CarpetTISAdditionSettings.netherPortalMaxSize != CarpetTISAdditionSettings.VANILLA_NETHER_PORTAL_MAX_SIZE)
		{
			limit = CarpetTISAdditionSettings.netherPortalMaxSize;
		}
		return limit;
	}
}
