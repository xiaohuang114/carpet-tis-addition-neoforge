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

package carpettisaddition.mixins.logger.phantom;

import carpettisaddition.logging.loggers.phantom.PhantomLogger;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import net.minecraft.server.level.ServerPlayer;

@Mixin(PhantomSpawner.class)
public abstract class PhantomSpawnerMixin
{
	// S4 / B-class: 1.21.1 dropped DifficultyInstance#getDifficulty() from tick (it now tests
	// isHarderThan(float), javap offset 205) and NeoForge moved the per-player gate into
	// EventHooks.firePlayerSpawnPhantoms(ServerPlayer, ServerLevel, BlockPos) at offset 146.
	// ServerLevel still flows in as a method argument there, so argsOnly=true keeps the same
	// "reset the share once per player iteration" behaviour.
	@ModifyVariable(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/neoforged/neoforge/event/EventHooks;firePlayerSpawnPhantoms(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)Lnet/neoforged/neoforge/event/entity/player/PlayerSpawnPhantomsEvent;"
			),
			argsOnly = true
	)
	private ServerLevel resetFlag_phantomLogger(ServerLevel serverWorld, @Share("once") LocalBooleanRef hasLogged)
	{
		hasLogged.set(false);
		return serverWorld;
	}

	// S4 / B-class: EntityType.create(Level) now occurs only once in tick (javap offset 368) and the
	// phantom count is no longer derived inline, so create/slice/ordinal=3 cannot resolve. The count
	// NeoForge supplies is stored at offset 353 (istore 19 <-
	// PlayerSpawnPhantomsEvent#getPhantomsToSpawn); it is the 5th int store of tick
	// (91, 167, 238, 243, 353, 356) and sits after every spawn gate, i.e. once per spawning player.
	@ModifyVariable(
			method = "tick",
			at = @At(value = "STORE"),
			// explicit local index: implicit int matching is ambiguous here (Mixin: "Found 2..5
			// candidate variables but exactly 1 is required"), slot 19 is the phantom count
			index = 19
	)
	private int doLog_phantomLogger(
			int amount,
			@Share("once") LocalBooleanRef hasLogged,
			@Local ServerPlayer player
	)
	{
		if (!hasLogged.get())
		{
			PhantomLogger.getInstance().onPhantomSpawn(player, amount);
			hasLogged.set(true);
		}
		return amount;
	}
}
