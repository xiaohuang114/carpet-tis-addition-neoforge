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

package carpettisaddition.mixins.helpers.mixin;

import carpettisaddition.helpers.mixin.IWorldOverrides;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.EntityGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import net.minecraft.world.level.CommonLevelAccessor;

@Mixin(
		value = CommonLevelAccessor.class,
		priority = 2000
)
public interface IWorldMixin extends EntityGetter
{
	/**
	 * @reason Interface injection is not supported by Mixin yet
	 * So here comes the @Overwrite
	 *
	 * @author Fallen_Breath
	 */
	@Overwrite
	default List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB box)
	{
		IWorldOverrides.getEntityCollisionsPre(entity, box);
		try
		{
			return IWorldOverrides.getEntityCollisionsModifyResult(
					entity, box,
					// vanilla copy
					EntityGetter.super.
							getEntityCollisions(entity, box)
			);
		}
		finally
		{
			IWorldOverrides.getEntityCollisionsPost(entity, box);
		}
	}
}
