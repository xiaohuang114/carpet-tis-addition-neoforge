/*
 * This file is part of the Carpet TIS Addition project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  Fallen_Breath and contributors
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

package carpettisaddition.mixins.rule.tickFreezeDeepCommand;

import carpettisaddition.helpers.rule.tickFreezeDeepCommand.DeepFreezableServerTickRateManager;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.server.level.DistanceManager;

@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin
{
	@Shadow @Final
	private
	ServerLevel level;

	// ref: https://github.com/gnembon/fabric-carpet/blob/72d9dd23e36fe5f71c44b5f098e79d5a58f589e8/src/main/java/carpet/mixins/ServerChunkCache_tickMixin.java#L102-L113
	@WrapWithCondition(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/server/level/DistanceManager;purgeStaleTickets()V"
			)
	)
	private boolean tickFreezeDeepCommand_applyTheDeepFreeze_ticketExpirationTicking(
			DistanceManager instance
	)
	{
		ServerTickRateManager serverTickRateManager = this.level.getServer().tickRateManager();
		if (serverTickRateManager instanceof DeepFreezableServerTickRateManager)
		{
			return !((DeepFreezableServerTickRateManager)serverTickRateManager).isDeeplyFrozen$TISCM();
		}
		return true;
	}
}
