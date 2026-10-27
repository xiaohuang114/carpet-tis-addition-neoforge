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

package carpettisaddition.settings;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.SettingsManager;
import carpettisaddition.CarpetTISAdditionMod;
import carpettisaddition.CarpetTISAdditionServer;
import carpettisaddition.CarpetTISAdditionSettings;
import carpettisaddition.network.TISCMProtocolRuleListener;
import carpettisaddition.settings.validator.AbstractValidator;
import carpettisaddition.translations.TranslationConstants;
import com.google.common.collect.Lists;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.lang.reflect.Constructor;

public class CarpetRuleRegistrar
{
	private static boolean hasRegistered = false;

	private final SettingsManager settingsManager;
	private final List<TISCMRule> rules = Lists.newArrayList();

	private CarpetRuleRegistrar(SettingsManager settingsManager)
	{
		this.settingsManager = settingsManager;
	}

	public static void register(SettingsManager settingsManager, Class<?> settingsClass)
	{
		if (hasRegistered)
		{
			throw new IllegalStateException("Already registered");
		}

		CarpetRuleRegistrar registrar = new CarpetRuleRegistrar(settingsManager);
		registrar.parseSettingsClass(settingsClass);
		registrar.registerToCarpet();
		TISCMRules.store(registrar.rules);
		hasRegistered = true;
	}

	public void parseSettingsClass(Class<?> settingsClass)
	{
		for (Field field : settingsClass.getDeclaredFields())
		{
			Rule rule = field.getAnnotation(Rule.class);
			if (rule != null)
			{
				this.parseRule(field, rule);
			}
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Class<? extends AbstractValidator>[] extractValidators(Rule rule)
	{
		List<Class<? extends AbstractValidator>> validators = Lists.newArrayList(rule.validators());
		if (Arrays.asList(rule.categories()).contains(CarpetTISAdditionSettings.TISCM_PROTOCOL))
		{
			validators.add(TISCMProtocolRuleListener.class);
		}
		return validators.toArray(new Class[0]);
	}

	@SuppressWarnings("rawtypes")
	private void parseRule(Field field, Rule rule)
	{
		Class<? extends AbstractValidator>[] validators = extractValidators(rule);
		CarpetRule<?> carpetRule = null;
		try
		{
			Class<?> ruleAnnotationClass = Class.forName("carpet.settings.ParsedRule$RuleAnnotation");
			Constructor<?> ctr1 = ruleAnnotationClass.getDeclaredConstructors()[0];
			ctr1.setAccessible(true);
			Object ruleAnnotation = ctr1.newInstance(false, null, null, null, rule.categories(), rule.options(), rule.strict(), "", validators);

			Class<?> parsedRuleClass = Class.forName("carpet.settings.ParsedRule");
			Constructor<?> ctr2 = parsedRuleClass.getDeclaredConstructors()[0];
			ctr2.setAccessible(true);
			carpetRule = (CarpetRule<?>)ctr2.newInstance(field, ruleAnnotation, this.settingsManager);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}

		this.rules.add(new TISCMRule(rule, field, carpetRule));
	}

	public void registerToCarpet()
	{
		for (TISCMRule rule : this.rules)
		{
			try
			{
				this.settingsManager.addCarpetRule(rule.getCarpetRule());
			}
			catch (UnsupportedOperationException e)
			{
				CarpetTISAdditionServer.LOGGER.warn("[TISCM] Failed to register rule {} to fabric carpet: {}", rule.getName(), e);
			}
		}
	}
}
