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

package carpettisaddition.mixins.rule.clientSettingsLostOnRespawnFix;

import carpettisaddition.CarpetTISAdditionSettings;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.level.ClientInformation;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin
{
	@Shadow public abstract void
			updateOptions(ClientInformation settings);

	@Unique
	@Nullable
	private ClientInformation lastClientSettings$TISCM = null;

	@Inject(
			method = "updateOptions",
			at = @At("TAIL")
	)
	private void storeClientSettingsC2SPacket(
			ClientInformation settings,
			CallbackInfo ci)
	{
		if (CarpetTISAdditionSettings.clientSettingsLostOnRespawnFix)
		{
			this.lastClientSettings$TISCM = settings;
		}
	}

	@Inject(method = "restoreFrom", at = @At("TAIL"))
	private void clientSettingsLostOnRespawnFix(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci)
	{
		if (CarpetTISAdditionSettings.clientSettingsLostOnRespawnFix)
		{
			var
					settings = ((ServerPlayerEntityMixin)(Object)oldPlayer).lastClientSettings$TISCM;
			if (settings != null)
			{
				this.updateOptions(settings);
			}
		}
	}
}
