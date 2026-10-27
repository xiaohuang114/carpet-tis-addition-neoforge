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

package carpettisaddition.helpers.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * Interface injection is not valid in at least java8,
 * so here comes a helper class for multiple mixins targeting {@link net.minecraft.world.level.LevelAccessor#getEntityCollisions}
 */
public class IWorldOverrides
{
	public static void getEntityCollisionsPre(@Nullable Entity entity, AABB box)
	{
		// do mixin @Inject here
	}

	public static void getEntityCollisionsPost(@Nullable Entity entity, AABB box)
	{
		// do mixin @Inject here
	}
	public static List<VoxelShape> getEntityCollisionsModifyResult(@Nullable Entity entity, AABB box, List<VoxelShape> result)
	{
		// do mixin @ModifyVariable here
		return result;
	}
}
