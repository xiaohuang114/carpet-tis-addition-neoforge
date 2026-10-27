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

package carpettisaddition.mixins.translations;

import carpettisaddition.translations.ServerPlayerEntityWithClientLanguage;
import carpettisaddition.translations.TISAdditionTranslations;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.level.ClientInformation;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin implements ServerPlayerEntityWithClientLanguage
{
	@Unique
	private String clientLanguage$TISCM = "en_US";

	@Inject(
			method = "updateOptions",
			at = @At("HEAD")
	)
	private void recordClientLanguage(
			ClientInformation settings,
			CallbackInfo ci)
	{
		this.clientLanguage$TISCM =
				settings.language();
	}

	@Override
	public String getClientLanguage$TISCM()
	{
		return this.clientLanguage$TISCM;
	}

	@Override
	public void setClientLanguage$TISCM(String clientLanguage)
	{
		this.clientLanguage$TISCM = clientLanguage;
	}

	/**
	 * This handle all TISCM translation on chat messages
	 */
	@ModifyVariable(
			method = {
					"sendSystemMessage(Lnet/minecraft/network/chat/Component;Z)V",
			},
			at = @At("HEAD"),
			argsOnly = true
	)
	private Component applyTISCarpetTranslationToChatMessage(Component message)
	{
		if (message instanceof MutableComponent)
		{
			message = TISAdditionTranslations.translate((MutableComponent)message, (ServerPlayer)(Object)this);
		}
		return message;
	}
}
