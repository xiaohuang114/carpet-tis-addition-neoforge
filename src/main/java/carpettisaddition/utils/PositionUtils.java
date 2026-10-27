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

package carpettisaddition.utils;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class PositionUtils
{
	public static BlockPos flooredBlockPos(Vec3 vec3d)
	{
		return BlockPos.containing(vec3d);
	}

	public static BoundingBox createBlockBox(BlockPos pos1, BlockPos pos2)
	{
		return BoundingBox.fromCorners(pos1, pos2);
	}

	public static ChunkPos flooredChunkPos(BlockPos pos)
	{
		return new ChunkPos(pos);
	}

	public static long packChunkPos(ChunkPos chunkPos)
	{
		return chunkPos.toLong();
	}
	public static long packChunkPos(BlockPos blockPos)
	{
		return ChunkPos.asLong(blockPos);
	}

	public static long packChunkPos(int x, int z)
	{
		return ChunkPos.asLong(x, z);
	}

	public static ChunkPos unpackChunkPos(long data)
	{
		return new ChunkPos(data);
	}

	public static int chunkPosX(ChunkPos chunkPos)
	{
		return chunkPos.x;
	}

	public static int chunkPosZ(ChunkPos chunkPos)
	{
		return chunkPos.z;
	}
}
