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

package carpettisaddition.mixins.rule.hopperNoItemCost;

import carpet.utils.WoolTool;
import carpettisaddition.CarpetTISAdditionSettings;
import carpettisaddition.commands.scounter.SupplierCounterCommand;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin extends RandomizableContainerBlockEntity
{
	protected HopperBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState)
	{
		super(blockEntityType, blockPos, blockState);
	}

	@Inject(
			method = "ejectItems",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/Container;setChanged()V",
					shift = At.Shift.AFTER
			)
	)
	private static
	void hopperNoItemCost_hopperOutputHook(
			CallbackInfoReturnable<Boolean> cir,
			@Local(argsOnly = true) Level world, @Local(argsOnly = true) BlockPos pos,
			@Local(argsOnly = true) HopperBlockEntity hopperInventory,
			@Local(ordinal = 0) int index, @Local(ordinal = 0) ItemStack itemStack
			, @Local(ordinal = 1) int prevCount
	)
	{
		if (!CarpetTISAdditionSettings.hopperNoItemCost)
		{
			return;
		}

		if (world == null)
		{
			return;
		}
		BlockPos hopperPos =
				pos;
		DyeColor wool_color = WoolTool.getWoolColorAtPosition(world, hopperPos.relative(Direction.UP));
		if (wool_color == null)
		{
			return;
		}
		int currentCount = itemStack.getCount();
		itemStack.setCount(prevCount);  // NOTES: empty_stack.copy() == EMPTY, so we need to setCount() before copy()
		ItemStack prevStack = itemStack.copy();
		itemStack.setCount(currentCount);
		ItemStack currentStack = hopperInventory.getItem(index);

		if (SupplierCounterCommand.getInstance().isActivated())
		{
			SupplierCounterCommand.getInstance().record(wool_color, prevStack, currentStack);
		}

		// restore the hopper inventory slot to the previous stack
		hopperInventory.setItem(index, prevStack);
	}
}
