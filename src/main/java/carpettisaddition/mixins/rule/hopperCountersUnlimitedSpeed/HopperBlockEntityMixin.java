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

package carpettisaddition.mixins.rule.hopperCountersUnlimitedSpeed;

import carpet.CarpetSettings;
import carpettisaddition.CarpetTISAdditionServer;
import carpettisaddition.CarpetTISAdditionSettings;
import carpettisaddition.utils.HopperCounterUtils;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.Container;
import net.minecraft.world.item.DyeColor;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import java.util.function.BooleanSupplier;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin extends RandomizableContainerBlockEntity
{
	@Unique
	private static final int OPERATION_LIMIT = Short.MAX_VALUE;
	protected HopperBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState)
	{
		super(blockEntityType, blockPos, blockState);
	}
	@Shadow
	private static boolean ejectItems(Level world, BlockPos blockPos, HopperBlockEntity hopper)
	{
		return false;
	}

	@Inject(
			method = "tryMoveItems",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;setCooldown(I)V",
					shift = At.Shift.AFTER
			)
	)
	private static void hopperCountersUnlimitedSpeed_impl(Level world, BlockPos blockPos, BlockState blockState, HopperBlockEntity hopperBlockEntity, BooleanSupplier booleanSupplier, CallbackInfoReturnable<Boolean> cir)
	{
		if (CarpetSettings.hopperCounters && CarpetTISAdditionSettings.hopperCountersUnlimitedSpeed)
		{
			HopperBlockEntity self =
					hopperBlockEntity;

			// hopper counter check
			DyeColor wool_color = HopperCounterUtils.getWoolColorForHopper(self);
			if (wool_color == null)
			{
				return;
			}
			// hopper counter check ends

			BlockPos hopperPos = self.getBlockPos();

			// unlimited speed
			for (int i = OPERATION_LIMIT - 1; i >= 0; i--)
			{
				boolean flag = false;

				// vanilla copy starts
				if (!hopperBlockEntity.isEmpty())
				{
					flag = ejectItems(world, blockPos, hopperBlockEntity);
				}
				if (!((HopperBlockEntityAccessor)hopperBlockEntity).invokeIsFull())
				{
					flag |= booleanSupplier.getAsBoolean();
				}
				// vanilla copy ends

				if (!flag)
				{
					break;
				}
				if (i == 0)
				{
					CarpetTISAdditionServer.LOGGER.warn("Hopper at {} exceeded hopperCountersUnlimitedSpeed operation limit {}", hopperPos, OPERATION_LIMIT);
				}
			}

			// no cooldown
			((HopperBlockEntityAccessor)hopperBlockEntity).invokeSetCooldown(0);
		}
	}

	// to avoid repeatedly extraction an item entity in onEntityCollided
	@Inject(
			method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z",
			at = @At("HEAD"),
			cancellable = true
	)
	private static void hopperCountersUnlimitedSpeed_dontExtractRemovedItem(Container inventory, ItemEntity itemEntity, CallbackInfoReturnable<Boolean> cir)
	{
		if (CarpetTISAdditionSettings.hopperCountersUnlimitedSpeed)
		{
			if (
					itemEntity.isRemoved()
			)
			{
				cir.setReturnValue(false);
			}
		}
	}
}
