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

package carpettisaddition.helpers.rule.minecartPlaceableOnGround;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;

public class MinecartPlaceableOnGroundImpl
{
	/**
	 * Reference: {@link net.minecraft.world.item.MinecartItem#useOn}
	 */
	public static InteractionResult placeMinecartOnGround(
			UseOnContext context,
			AbstractMinecart.Type minecartType
	)
	{
		Player player = context.getPlayer();
		Level world = context.getLevel();
		ItemStack itemStack = context.getItemInHand();
		Vec3 vec3d = context.getClickLocation();

		if (
				world instanceof ServerLevel serverWorld
		)
		{
			AbstractMinecart minecartEntity = AbstractMinecart.createMinecart(serverWorld, vec3d.x(), vec3d.y(), vec3d.z(), minecartType, itemStack, player);

			if (player != null)
			{
				minecartEntity.setYRot(player.getYRot());
			}

			// check if the space of the minecart entity is occupied, like what boat placement does
			if (!isMinecartPositionValid(world, minecartEntity))
			{
				return InteractionResult.FAIL;
			}

			world.addFreshEntity(minecartEntity);
			world.gameEvent(context.getPlayer(), net.minecraft.world.level.gameevent.GameEvent.ENTITY_PLACE, context.getClickedPos());
		}
		else
		{
			// For proper client swing-hand animation. AbstractMinecart.create() needs ServerLevel in mc1.20.4+, we can't use it on the clientside
			// Assumption: The simply-initialized minecart entity has the same size of the serverside-created one,
			// i.e., no nbt scaling tricks, modded new minecart variant
			if (!isMinecartPositionValid(world, new Minecart(world, vec3d.x(), vec3d.y(), vec3d.z())))
			{
				return InteractionResult.FAIL;
			}
		}

		itemStack.shrink(1);
		return InteractionResult.sidedSuccess(world.isClientSide());
	}

	private static boolean isMinecartPositionValid(Level world, AbstractMinecart minecartEntity)
	{
		// reference: net.minecraft.item.BoatItem#use
		return world.noCollision(
				minecartEntity,
				minecartEntity.getBoundingBox()
		);
	}
}
