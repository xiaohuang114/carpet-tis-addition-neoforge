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

package carpettisaddition.mixins.rule.tooledTNT;

import carpettisaddition.helpers.rule.tooledTNT.TooledTNTHelper;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.state.BlockBehaviour;

@Mixin(
		BlockBehaviour.class
)
public abstract class ExplosionMixin
{
	/**
	 * See {@link OptimizedExplosionMixin} for mixin at carpet in for this rule
	 */
	@ModifyExpressionValue(
			method = "onExplosionHit",
			at = @At(
					value = "FIELD",
					target = "Lnet/minecraft/world/item/ItemStack;EMPTY:Lnet/minecraft/world/item/ItemStack;"
			),
			allow = 2
	)
	private ItemStack useTheToolInYourHand(
			ItemStack itemStack
			, @Local(argsOnly = true) Explosion explosion
	)
	{

		return TooledTNTHelper.getMainHandItemOfCausingEntity(explosion).orElse(itemStack);
	}
}
