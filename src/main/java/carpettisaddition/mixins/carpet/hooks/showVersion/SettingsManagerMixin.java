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

package carpettisaddition.mixins.carpet.hooks.showVersion;

import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;
import carpet.utils.Translations;
import carpettisaddition.CarpetTISAdditionMod;
import carpettisaddition.utils.Messenger;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static carpettisaddition.CarpetTISAdditionMod.MOD_NAME;

@Mixin(SettingsManager.class)
public abstract class SettingsManagerMixin
{
	@Inject(
			method = "listAllSettings",

			at = @At(
					value = "INVOKE",
					target = "Lcarpet/api/settings/SettingsManager;getCategories()Ljava/lang/Iterable;"
			),
			remap = false
	)
	private void printAdditionVersion(CommandSourceStack source, CallbackInfoReturnable<Integer> cir)
	{
		SettingsManager self = (SettingsManager)(Object)this;
		if (self != CarpetServer.settingsManager)
		{
			return;
		}
		Messenger.tell(
				source,
				Messenger.c(
						String.format("g %s ", MOD_NAME),
						String.format("g %s: ", Translations.tr("carpet.settings.command.version", "version")),
						String.format("g %s", CarpetTISAdditionMod.getVersion())
				)
		);
	}
}
