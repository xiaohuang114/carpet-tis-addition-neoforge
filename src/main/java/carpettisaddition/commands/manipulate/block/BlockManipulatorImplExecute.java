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

package carpettisaddition.commands.manipulate.block;

import carpettisaddition.CarpetTISAdditionSettings;
import carpettisaddition.translations.TranslationContext;
import carpettisaddition.translations.Translator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.Biome;

class BlockManipulatorImplExecute extends TranslationContext
{
	public BlockManipulatorImplExecute(Translator translator)
	{
		super(translator);
	}

	public void executeBlockEvent(CommandSourceStack source, BlockPos blockPos, int type, int data)
	{
		ServerLevel world = source.getLevel();
		BlockState blockState = world.getBlockState(blockPos);
		blockState.triggerEvent(world, blockPos, type, data);
		source.getServer().getPlayerList().broadcast(
				null,
				blockPos.getX(),
				blockPos.getY(),
				blockPos.getZ(),
				CarpetTISAdditionSettings.blockEventPacketRange,
				world.dimension(),
				new ClientboundBlockEventPacket(blockPos, blockState.getBlock(), type, data)
		);
	}

	public void executeTileTickAt(CommandSourceStack source, BlockPos blockPos)
	{
		ServerLevel world = source.getLevel();
		BlockState blockState = world.getBlockState(blockPos);
		FluidState fluidState = world.getFluidState(blockPos);

		blockState.tick(world, blockPos, world.getRandom());
		fluidState.tick(
				world, blockPos
		);
	}

	public void executeRandomTickAt(CommandSourceStack source, BlockPos blockPos)
	{
		ServerLevel world = source.getLevel();
		BlockState blockState = world.getBlockState(blockPos);
		FluidState fluidState = world.getFluidState(blockPos);

		// ref: net.minecraft.server.level.ServerLevel#tickChunk
		blockState.randomTick(world, blockPos, world.getRandom());
		fluidState.randomTick(world, blockPos, world.getRandom());
	}

	public void executePrecipitationTickAt(CommandSourceStack source, BlockPos blockPos)
	{
		ServerLevel world = source.getLevel();
		BlockState blockState = world.getBlockState(blockPos);

		// ref: net.minecraft.server.level.ServerLevel#tickChunk
		Biome biome = world.getBiome(blockPos.above()).value();

		Biome.Precipitation precipitation = biome.getPrecipitationAt(
				blockPos
		);
		if (precipitation != Biome.Precipitation.NONE)
		{
			blockState.getBlock().handlePrecipitation(blockState, world, blockPos, precipitation);
		}
	}
}
