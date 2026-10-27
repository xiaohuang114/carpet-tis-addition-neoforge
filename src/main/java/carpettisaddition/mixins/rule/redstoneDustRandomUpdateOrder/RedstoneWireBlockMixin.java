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

package carpettisaddition.mixins.rule.redstoneDustRandomUpdateOrder;

import carpettisaddition.CarpetTISAdditionSettings;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(
		RedStoneWireBlock.class
)
public abstract class RedstoneWireBlockMixin
{
	@Unique
	private final Random random$TISCM = new Random();
	@ModifyVariable(
			method = "updatePowerStrength",
			at = @At(
					value = "INVOKE",
					target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"
			)
	)
	private Set<BlockPos> letsMakeTheOrderUnpredictable(Set<BlockPos> set)
	{
		if (CarpetTISAdditionSettings.redstoneDustRandomUpdateOrder)
		{
			List<BlockPos> list = Lists.newArrayList(set);
			Collections.shuffle(list, this.random$TISCM);
			set = Sets.newLinkedHashSet(list);
		}
		return set;
	}
}
