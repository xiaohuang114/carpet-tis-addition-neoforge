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

package carpettisaddition.translations;
import carpet.CarpetSettings;

import carpettisaddition.CarpetTISAdditionServer;
import carpettisaddition.CarpetTISAdditionSettings;
import carpettisaddition.mixins.translations.StyleAccessor;
import carpettisaddition.utils.Messenger;
import com.google.common.collect.Maps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static carpettisaddition.translations.TranslationConstants.*;

public class TISAdditionTranslations
{
	/**
	 * language -> (key -> content)
	 */
	private static final Map<String, Map<String, String>> translationStorage = Maps.newLinkedHashMap();

	public static void loadTranslations()
	{
		TranslationLoader.loadTranslations(translationStorage);
	}

	public static Collection<String> getLanguages()
	{
		return Collections.unmodifiableSet(translationStorage.keySet());
	}

	/**
	 * @param lang the language
	 * @return key -> translated content (translated formatting string)
	 */
	@NotNull
	public static Map<String, String> getTranslations(String lang)
	{
		return translationStorage.getOrDefault(lang, Collections.emptyMap());
	}

	public static String getServerLanguage()
	{
		return CarpetSettings.language.equalsIgnoreCase("none") ? DEFAULT_LANGUAGE : CarpetSettings.language;
	}

	/**
	 * key -> translated formatting string
	 */
	@Nullable
	public static String getTranslationString(String lang, String key)
	{
		return getTranslations(lang.toLowerCase()).get(key);
	}

	public static boolean hasTranslation(String key)
	{
		return getTranslationString(DEFAULT_LANGUAGE, key) != null;
	}

	// please ensure that this text is a TranslatableComponent
	public static boolean hasTranslation(MutableComponent text)
	{
		return hasTranslation(((TranslatableContents)text.getContents()).getKey());
	}

	public static MutableComponent translate(MutableComponent text, String lang)
	{
		return translateText(text, lang);
	}

	public static MutableComponent translate(MutableComponent text)
	{
		return translate(text, getServerLanguage());
	}

	public static MutableComponent translate(MutableComponent text, ServerPlayer player)
	{
		if (CarpetTISAdditionSettings.ultraSecretSetting.equals("translation"))
		{
			return translate(text);
		}
		return translate(text, ((ServerPlayerEntityWithClientLanguage)player).getClientLanguage$TISCM());
	}

	private static MutableComponent translateText(MutableComponent text, @NotNull String lang)
	{
		// quick scan to check if any required translation exists
		boolean[] translationRequired = new boolean[]{false};
		forEachTISCMTranslationText(text, lang, (txt, msgKeyString) -> {
			translationRequired[0] = true;
			return txt;
		});
		if (!translationRequired[0])
		{
			return text;
		}

		// make a copy of the text, and apply translation
		return forEachTISCMTranslationText(Messenger.copy(text), lang, (txt, msgKeyString) -> {
			TranslatableContents content = (TranslatableContents)txt.getContents();
			String txtKey = content.getKey();
			Object[] txtArgs = content.getArgs();

			if (msgKeyString == null)
			{
				CarpetTISAdditionServer.LOGGER.warn("TISCM: Unknown translation key {}", txtKey);
				return txt;
			}

			MutableComponent newText;
			try
			{
				newText = Messenger.format(msgKeyString, txtArgs);
			}
			catch (IllegalArgumentException e)
			{
				newText = Messenger.s(msgKeyString);
			}

			// migrating text data
			newText.getSiblings().addAll(txt.getSiblings());
			newText.setStyle(txt.getStyle());

			return newText;
		});
	}

	private static MutableComponent forEachTISCMTranslationText(MutableComponent text, @NotNull String lang, TextModifier modifier)
	{
		if (
				text.getContents() instanceof TranslatableContents
		)
		{
			TranslatableContents translatableText = (TranslatableContents)text.getContents();

			// translate arguments
			Object[] args = translatableText.getArgs();
			for (int i = 0; i < args.length; i++)
			{
				Object arg = args[i];
				if (arg instanceof MutableComponent)
				{
					MutableComponent newText = forEachTISCMTranslationText((MutableComponent)arg, lang, modifier);
					if (newText != arg)
					{
						args[i] = newText;
					}
				}
			}

			// do translation logic
			if (translatableText.getKey().startsWith(TRANSLATION_KEY_PREFIX))
			{
				String msgKeyString = getTranslationString(lang, translatableText.getKey());
				if (msgKeyString == null && !lang.equals(DEFAULT_LANGUAGE))
				{
					msgKeyString = getTranslationString(DEFAULT_LANGUAGE, translatableText.getKey());
				}
				text = modifier.apply(
						text,
						msgKeyString
				);
			}
		}

		// translate hover text
		@SuppressWarnings("RedundantCast")  // in mc1.21.9+, Style is a final class, so we need to cast it to Object first
		HoverEvent hoverEvent = ((StyleAccessor)(Object)text.getStyle()).getHoverEvent$TISCM();
		if (hoverEvent != null)
		{
			MutableComponent oldHoverText = Util.make(() -> {
				Object hoverEventValue = hoverEvent.getValue(hoverEvent.getAction());
				if (hoverEvent.getAction() == HoverEvent.Action.SHOW_TEXT && hoverEventValue instanceof MutableComponent)
				{
					 return (MutableComponent)hoverEventValue;
				}
				return null;
			});

			if (oldHoverText != null)
			{
				MutableComponent newHoverText = forEachTISCMTranslationText(oldHoverText, lang, modifier);
				if (newHoverText != oldHoverText)
				{
					Messenger.hover(text, newHoverText);
				}
			}
		}

		// translate sibling texts
		List<Component> siblings = text.getSiblings();
		for (int i = 0; i < siblings.size(); i++)
		{
			Component sibling = siblings.get(i);
			MutableComponent newText = forEachTISCMTranslationText((MutableComponent)sibling, lang, modifier);
			if (newText != sibling)
			{
				siblings.set(i, newText);
			}
		}
		return text;
	}

	@FunctionalInterface
	private interface TextModifier
	{
		MutableComponent apply(
				MutableComponent translatableText,
				@Nullable String msgKeyString
		);
	}
}
