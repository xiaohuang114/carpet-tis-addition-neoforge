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

package carpettisaddition.mixins.rule.fakePlayerNameExtra;

import carpet.commands.PlayerCommand;
import carpettisaddition.CarpetTISAdditionSettings;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(PlayerCommand.class)
public abstract class PlayerCommandMixin
{
	@Unique
	private static String getDecoratedPlayerName(String playerName)
	{
		String rulePrefix = CarpetTISAdditionSettings.fakePlayerNamePrefix;
		String ruleSuffix = CarpetTISAdditionSettings.fakePlayerNameSuffix;
		if (!rulePrefix.equals(CarpetTISAdditionSettings.fakePlayerNameNoExtra) && !playerName.startsWith(rulePrefix))
		{
			playerName = rulePrefix + playerName;
		}
		if (!ruleSuffix.equals(CarpetTISAdditionSettings.fakePlayerNameNoExtra) && !playerName.endsWith(ruleSuffix))
		{
			playerName = playerName + ruleSuffix;
		}
		return playerName;
	}

	@ModifyExpressionValue(
			method = "spawn",
			at = @At(
					value = "INVOKE",
					target = "Lcom/mojang/brigadier/arguments/StringArgumentType;getString(Lcom/mojang/brigadier/context/CommandContext;Ljava/lang/String;)Ljava/lang/String;"
			),
			remap = false
	)
	private static String getStringWithPrefixAtSpawn(String value)
	{
		return getDecoratedPlayerName(value);
	}

	@ModifyExpressionValue(
			method = "cantSpawn",
			at = @At(
					value = "INVOKE",
					target = "Lcom/mojang/brigadier/arguments/StringArgumentType;getString(Lcom/mojang/brigadier/context/CommandContext;Ljava/lang/String;)Ljava/lang/String;"
			),
			require = 1,
			remap = false
	)
	private static String getStringWithPrefixAtCantSpawn(String value)
	{
		return getDecoratedPlayerName(value);
	}
}
