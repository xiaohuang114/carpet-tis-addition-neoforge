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

package carpettisaddition.commands.manipulate.container.controller;

import carpettisaddition.commands.CommandTreeContext;
import carpettisaddition.utils.Messenger;
import carpettisaddition.utils.compat.DimensionWrapper;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.world.level.block.Block;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ticks.TickPriority;
import org.jetbrains.annotations.Nullable;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos;
import static net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos;
import static net.minecraft.commands.arguments.blocks.BlockStateArgument.block;
import static net.minecraft.commands.arguments.blocks.BlockStateArgument.getBlock;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import net.minecraft.world.ticks.LevelTicks;

public class TileTickQueueController extends AbstractContainerController
{
	public TileTickQueueController()
	{
		super("tile_tick");
	}

	private int remove(
			LevelTicks<?> tickScheduler,
			BlockPos blockPos)
	{
		BoundingBox blockBox =
				BoundingBox.fromCorners
						(blockPos, blockPos.offset(1, 1, 1));
		int sizeBefore = tickScheduler.count();
		tickScheduler.clearArea(blockBox);
		int sizeAfter = tickScheduler.count();
		return sizeBefore - sizeAfter;
	}

	private int removeAt(CommandSourceStack source, BlockPos blockPos)
	{
		int counter = 0;
		counter += this.remove(source.getLevel().getBlockTicks(), blockPos);
		counter += this.remove(source.getLevel().getFluidTicks(), blockPos);
		Messenger.tell(source, tr("removed", counter), true);
		return counter;
	}

	private int addTileTickEvent(CommandContext<CommandSourceStack> context, @Nullable Object priorityArg) throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		BlockPos blockPos = getLoadedBlockPos(context, "pos");
		Block block = getBlock(context, "block").getState().getBlock();
		int delay = getInteger(context, "delay");
		TickPriority priority = TickPriority.NORMAL;
		if (priorityArg instanceof TickPriority)
		{
			priority = (TickPriority)priorityArg;
		}
		if (priorityArg instanceof Integer)
		{
			priority = TickPriority.byValue((Integer)priorityArg);
		}

		source.getLevel().
				scheduleTick
				(blockPos, block, delay, priority);
		Messenger.tell(source, tr(
				"scheduled",
				Messenger.fancy(tr("item_name"), tr("item_description", Messenger.block(block), delay, priority.getValue(), priority), null),
				Messenger.coord(blockPos, DimensionWrapper.of(source.getLevel()))
		), true);
		return 1;
	}

	@Override
	public ArgumentBuilder<CommandSourceStack, ?> getCommandNode(CommandTreeContext context)
	{
		return super.getCommandNode(context).
				then(literal("remove").
						then(argument("pos", blockPos()).
								executes(c -> this.removeAt(c.getSource(), getLoadedBlockPos(c, "pos")))
						)
				).
				then(literal("add").
						then(argument("pos", blockPos()).
								then(argument("block", block(
												context.commandBuildContext
										)).
										then(argument("delay", integer()).
												executes(c -> this.addTileTickEvent(c, null)).
												then(argument("priority", integer(TickPriority.EXTREMELY_HIGH.getValue(), TickPriority.EXTREMELY_LOW.getValue())).
														executes(c -> this.addTileTickEvent(c, getInteger(c, "priority")))
												)
										)
								)
						)
				);
	}
}
