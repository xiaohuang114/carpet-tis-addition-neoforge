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

package carpettisaddition.mixins.logger;

import carpet.logging.HUDLogger;
import carpet.logging.Logger;
import carpettisaddition.logging.loggers.HUDLoggerButCanLogToChat;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import net.minecraft.network.chat.Component;

@Mixin(HUDLogger.class)
public abstract class HUDLoggerMixin extends Logger implements HUDLoggerButCanLogToChat
{
	public HUDLoggerMixin(
			Field acceleratorField, String logName, String def, String[] options
			, boolean strictOptions
	)
	{
		super(
				acceleratorField,
				logName, def, options
				, strictOptions
		);
	}

	@Unique
	private final ThreadLocal<Boolean> shouldLogToChatThisTime$TISCM = ThreadLocal.withInitial(() -> false);

	@Override
	public void setShouldLogToChat$TISCM(boolean shouldLogToChat)
	{
		if (shouldLogToChat)
		{
			this.shouldLogToChatThisTime$TISCM.set(true);
		}
		else
		{
			this.shouldLogToChatThisTime$TISCM.remove();
		}
	}

	@Inject(method = "sendPlayerMessage", at = @At("HEAD"), cancellable = true)
	private void pleaseBehaveAsARegularLoggerThisTime(
			ServerPlayer player,
			Component[] messages,
			CallbackInfo ci
	)
	{
		if (this.shouldLogToChatThisTime$TISCM.get())
		{
			super.sendPlayerMessage(player, messages);
			ci.cancel();
		}
	}
}
