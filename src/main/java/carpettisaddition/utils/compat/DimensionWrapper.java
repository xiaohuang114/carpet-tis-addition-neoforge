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

package carpettisaddition.utils.compat;

import carpettisaddition.utils.EntityUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Objects;
import net.minecraft.resources.ResourceKey;

/*
 * A wrapper class to deal with dimension type class differences between minecraft version:
 * - DimensionType in 1.15-
 * - ResourceKey<Level> in 1.16+
 */
public class DimensionWrapper
{
	public static final DimensionWrapper OVERWORLD = of(
			Level.OVERWORLD
	);
	public static final DimensionWrapper THE_NETHER = of(
			Level.NETHER
	);
	public static final DimensionWrapper THE_END = of(
			Level.END
	);
	private final ResourceKey<Level>
			dimensionType;

	private DimensionWrapper(
			ResourceKey<Level> dimensionType
	)
	{
		this.dimensionType = dimensionType;
	}

	/**
	 * Warning: mc version dependent
	 */
	public static DimensionWrapper of(
			ResourceKey<Level> dimensionType
	)
	{
		return new DimensionWrapper(dimensionType);
	}

	public static DimensionWrapper of(Level world)
	{
		return new DimensionWrapper(
				world.dimension()
		);
	}

	public static DimensionWrapper of(Entity entity)
	{
		return of(EntityUtils.getEntityWorld(entity));
	}

	/**
	 * Warning: mc version dependent
	 */
	public ResourceKey<Level> getValue()
	{
		return this.dimensionType;
	}

	public ResourceLocation getIdentifier()
	{
		return this.dimensionType.location();
	}

	public String getIdentifierString()
	{
		return this.getIdentifier().toString();
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DimensionWrapper that = (DimensionWrapper) o;
		return Objects.equals(dimensionType, that.dimensionType);
	}

	@Override
	public int hashCode()
	{
		return this.dimensionType.hashCode();
	}

	@Deprecated
	@Override
	public String toString()
	{
		return this.getIdentifierString();
	}
}
