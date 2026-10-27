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

package carpettisaddition.mixins.logger.microtiming.events;

import carpettisaddition.logging.loggers.microtiming.MicroTimingLoggerManager;
import carpettisaddition.logging.loggers.microtiming.enums.EventType;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import java.util.Iterator;
import java.util.Set;

public abstract class EmitBlockUpdateMixins
{
	@Mixin(DiodeBlock.class)
	public static abstract class AbstractRedstoneGateBlockMixin
	{
		@Inject(method = "updateNeighborsInFront", at = @At("HEAD"))
		private void startEmitBlockUpdate(Level world, BlockPos pos, BlockState state, CallbackInfo ci)
		{
			MicroTimingLoggerManager.onEmitBlockUpdate(world, (DiodeBlock)(Object)this, pos, EventType.ACTION_START, "updateTarget");
		}

		@Inject(method = "updateNeighborsInFront", at = @At("RETURN"))
		private void endEmitBlockUpdate(Level world, BlockPos pos, BlockState state, CallbackInfo ci)
		{
			MicroTimingLoggerManager.onEmitBlockUpdate(world, (DiodeBlock)(Object)this, pos, EventType.ACTION_END, "updateTarget");
		}
	}

	@Mixin(ObserverBlock.class)
	public static abstract class ObserverBlockMixin
	{
		@Inject(method = "updateNeighborsInFront", at = @At("HEAD"))
		private void startEmitBlockUpdate(Level world, BlockPos pos, BlockState state, CallbackInfo ci)
		{
			MicroTimingLoggerManager.onEmitBlockUpdate(world, (ObserverBlock)(Object)this, pos, EventType.ACTION_START, "updateNeighbors");
		}

		@Inject(method = "updateNeighborsInFront", at = @At("RETURN"))
		private void endEmitBlockUpdate(Level world, BlockPos pos, BlockState state, CallbackInfo ci)
		{
			MicroTimingLoggerManager.onEmitBlockUpdate(world, (ObserverBlock)(Object)this, pos, EventType.ACTION_END, "updateNeighbors");
		}
	}

	@Mixin(PoweredRailBlock.class)
	public static abstract class PoweredRailBlockMixin
	{
		@Inject(
				method = "updateState",
				at = @At(
						value = "INVOKE",
						shift = At.Shift.AFTER,
						target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
				)
		)
		private void startEmitBlockUpdate(BlockState state, Level world, BlockPos pos, Block neighbor, CallbackInfo ci)
		{
			MicroTimingLoggerManager.onEmitBlockUpdate(world, (PoweredRailBlock)(Object)this, pos, EventType.ACTION_START, "updateBlockState");
		}

		@Inject(
				method = "updateState",
				at = @At("RETURN")
		)
		private void endEmitBlockUpdate(
				BlockState state, Level world, BlockPos pos, Block neighbor, CallbackInfo ci,
				@Local(ordinal = 0) boolean bl,
				@Local(ordinal = 1) boolean bl2
		)
		{
			if (bl2 != bl)  // vanilla copy, if power state should be changed
			{
				MicroTimingLoggerManager.onEmitBlockUpdate(world, (PoweredRailBlock) (Object) this, pos, EventType.ACTION_END, "updateBlockState");
			}
		}
	}

	@Mixin(LeverBlock.class)
	public static abstract class LeverBlockMixin
	{
		@Inject(method = "updateNeighbours", at = @At("HEAD"))
		private void startEmitBlockUpdate(BlockState state, Level world, BlockPos pos, CallbackInfo ci)
		{
			MicroTimingLoggerManager.onEmitBlockUpdate(world, (LeverBlock)(Object)this, pos, EventType.ACTION_START, "updateNeighbors");
		}

		@Inject(method = "updateNeighbours", at = @At("RETURN"))
		private void endEmitBlockUpdate(BlockState state, Level world, BlockPos pos, CallbackInfo ci)
		{
			MicroTimingLoggerManager.onEmitBlockUpdate(world, (LeverBlock)(Object)this, pos, EventType.ACTION_END, "updateNeighbors");
		}
	}

	@Mixin(ButtonBlock.class)
	public static abstract class AbstractButtonBlockMixin
	{
		@Inject(method = "updateNeighbours", at = @At("HEAD"))
		private void startEmitBlockUpdate(BlockState state, Level world, BlockPos pos, CallbackInfo ci)
		{
			MicroTimingLoggerManager.onEmitBlockUpdate(world, (ButtonBlock)(Object)this, pos, EventType.ACTION_START, "updateNeighbors");
		}

		@Inject(method = "updateNeighbours", at = @At("RETURN"))
		private void endEmitBlockUpdate(BlockState state, Level world, BlockPos pos, CallbackInfo ci)
		{
			MicroTimingLoggerManager.onEmitBlockUpdate(world, (ButtonBlock)(Object)this, pos, EventType.ACTION_END, "updateNeighbors");
		}
	}

	@Mixin(BasePressurePlateBlock.class)
	public static abstract class AbstractPressurePlateBlockMixin
	{
		@Inject(method = "updateNeighbours", at = @At("HEAD"))
		private void startEmitBlockUpdate(Level world, BlockPos pos, CallbackInfo ci)
		{
			MicroTimingLoggerManager.onEmitBlockUpdate(world, (BasePressurePlateBlock)(Object)this, pos, EventType.ACTION_START, "updateNeighbors");
		}

		@Inject(method = "updateNeighbours", at = @At("RETURN"))
		private void endEmitBlockUpdate(Level world, BlockPos pos, CallbackInfo ci)
		{
			MicroTimingLoggerManager.onEmitBlockUpdate(world, (BasePressurePlateBlock)(Object)this, pos, EventType.ACTION_END, "updateNeighbors");
		}
	}

	@Mixin(TripWireHookBlock.class)
	public static abstract class TripwireHookBlockMixin
	{
		@Inject(method = "notifyNeighbors", at = @At("HEAD"))
		private
		static
		void startEmitBlockUpdate(
				Block block,
				Level world, BlockPos pos, Direction direction, CallbackInfo ci
		)
		{
			MicroTimingLoggerManager.onEmitBlockUpdate(world, block, pos, EventType.ACTION_START, "updateNeighborsOnAxis");
		}

		@Inject(method = "notifyNeighbors", at = @At("RETURN"))
		private
		static
		void endEmitBlockUpdate(
				Block block,
				Level world, BlockPos pos, Direction direction, CallbackInfo ci
		)
		{
			MicroTimingLoggerManager.onEmitBlockUpdate(world, block, pos, EventType.ACTION_END, "updateNeighborsOnAxis");
		}
	}

	/**
	 * Use larger priority, so it injects after the @ModifyVariable in
	 * {@link carpettisaddition.mixins.rule.redstoneDustRandomUpdateOrder.RedstoneWireBlockMixin}
	 */
	@Mixin(
			value = RedStoneWireBlock.class,
			priority = 2000
	)
	public static abstract class RedstoneWireBlockMixin
	{
		// TODO: support ExperimentalRedstoneController in mc 1.21.2+

		@Inject(
				method = "updatePowerStrength",
				at = @At(
						value = "INVOKE",
						target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"
				)
		)
		private void startEmitBlockUpdate(
				CallbackInfo ci, @Local Set<BlockPos> collection,
				@Local(argsOnly = true) Level world, @Local(argsOnly = true) BlockPos pos
		)
		{
			MicroTimingLoggerManager.onEmitBlockUpdateRedstoneDust(
					world,
					(RedStoneWireBlock)(Object)this,
					pos, EventType.ACTION_START, "update", collection
			);
		}
		@Inject(
				method = "updatePowerStrength",
				at = @At(
						value = "INVOKE",
						target = "Ljava/util/Iterator;hasNext()Z"
				)
		)
		private void endEmitBlockUpdate(
				CallbackInfo ci,
				@Local(argsOnly = true) Level world,
				@Local(argsOnly = true) BlockPos pos,
				@Local Iterator<BlockPos> iterator
		)
		{
			if (!iterator.hasNext())
			{
				MicroTimingLoggerManager.onEmitBlockUpdateRedstoneDust(
						world,
						(RedStoneWireBlock)(Object)this,
						pos, EventType.ACTION_END, "update", null
				);
			}
		}
	}
}
