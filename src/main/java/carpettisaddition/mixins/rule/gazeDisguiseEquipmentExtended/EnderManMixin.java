/*
 * This file is part of the Carpet TIS Addition project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  Fallen_Breath and contributors
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

package carpettisaddition.mixins.rule.gazeDisguiseEquipmentExtended;

import carpettisaddition.CarpetTISAdditionSettings;
import carpettisaddition.helpers.rule.gazeDisguiseEquipmentExtended.GazeDisguiseEquipmentExtendedHelper;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Used in mc1.21.2-
 */
@Mixin(EnderMan.class)
public abstract class EnderManMixin
{
	// S4 / B-class: 1.21.1 Neoforge patched EnderMan#isLookingAtMe to call
	// CommonHooks.shouldSuppressEnderManAnger(this, player, armorStack); the ItemStack.is(Item)
	// carved-pumpkin test no longer exists in the target class, so ModifyReceiver has nothing to
	// hook. Same intent, new anchor: rewrite the armor stack local (slot 2) right after it is
	// stored from Player#getInventory().armor.get(3), so the NeoForge hook receives a carved
	// pumpkin for any gaze-disguise item. Handler body is unchanged.
	@ModifyVariable(
			method = "isLookingAtMe",
			at = @At(
					value = "STORE",
					ordinal = 0
			)
	)
	private ItemStack gazeDisguiseEquipmentExtended_pretendThisIsACarvedPumpkin(
			ItemStack playerWearingItemStack
	)
	{
		if (CarpetTISAdditionSettings.gazeDisguiseEquipmentExtended)
		{
			if (GazeDisguiseEquipmentExtendedHelper.isGazeDisguiseEquipmentExtendedItem(playerWearingItemStack))
			{
				playerWearingItemStack = new ItemStack(Items.CARVED_PUMPKIN);
			}
		}
		return playerWearingItemStack;
	}
}
