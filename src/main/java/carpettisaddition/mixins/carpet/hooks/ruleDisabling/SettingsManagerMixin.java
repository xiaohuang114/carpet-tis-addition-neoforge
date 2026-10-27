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

package carpettisaddition.mixins.carpet.hooks.ruleDisabling;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.SettingsManager;
import carpettisaddition.settings.TISCMRule;
import carpettisaddition.settings.TISCMRules;
import carpettisaddition.utils.Messenger;
import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Mixin(SettingsManager.class)
public abstract class SettingsManagerMixin
{
	@ModifyVariable(
			method = "displayRuleMenu",
			at = @At(
					value = "INVOKE",
					target = "Lcarpet/api/settings/CarpetRule;categories()Ljava/util/Collection;",
					ordinal = 0,
					remap = false
			),
			remap = false,
			argsOnly = true
	)
	private CarpetRule<?> addTiscmRuleDisablingMessage(
			CarpetRule<?> carpetRule,
			@Local(argsOnly = true) CommandSourceStack source
	)
	{
		TISCMRules.get(carpetRule).
				flatMap(SettingsManagerMixin::getTiscmRuleDisableMessage).
				ifPresent(lines -> lines.forEach(msg -> Messenger.tell(source, msg)));
		return carpetRule;
	}

	@Unique
	private static Optional<List<MutableComponent>> getTiscmRuleDisableMessage(TISCMRule tiscmRule)
	{
		List<MutableComponent> lines = Lists.newArrayList();
		Consumer<List<String>> addReasons = reasons -> reasons.forEach(
				reason -> lines.add(Messenger.formatting(Messenger.s("- " + reason), ChatFormatting.DARK_RED))
		);
		if (!tiscmRule.worksForCurrentMCVersion())
		{
			lines.add(Messenger.formatting(Messenger.tr("carpettisaddition.misc.rule_disabling.unmatched_minecraft"), ChatFormatting.RED));
			addReasons.accept(tiscmRule.getMCVersionFailReasons());
		}
		else if (!tiscmRule.allRestrictionsSatisfied())
		{
			lines.add(Messenger.formatting(Messenger.tr("carpettisaddition.misc.rule_disabling.unsatisfied_restriction"), ChatFormatting.RED));
			addReasons.accept(tiscmRule.getAllRestrictionsFailReasons());
		}
		return lines.isEmpty() ? Optional.empty() : Optional.of(lines);
	}
}
