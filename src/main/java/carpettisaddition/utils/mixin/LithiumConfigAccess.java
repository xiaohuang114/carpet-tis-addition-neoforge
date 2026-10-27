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

package carpettisaddition.utils.mixin;

import carpettisaddition.CarpetTISAdditionMod;
import carpettisaddition.utils.ModIds;
import net.caffeinemc.mods.lithium.common.config.LithiumConfig;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import net.caffeinemc.mods.lithium.common.config.Option;

public class LithiumConfigAccess
{
	// TIS-S6 (B-S5-2): 原来这里是
	//     @Nullable private static final Object config = loadLithiumConfig();
	// 静态字段初始化会在类首次被使用时执行，而本类最早的使用点在 Mixin 的 PREPARE 阶段
	// （BasicLithiumConditionTester -> isLithiumMixinRuleEnabled），那时 NeoForge 的 ModList 还没建立，
	// ModList.get() 返回 null -> isLithiumLoaded() NPE -> <clinit> 抛 ExceptionInInitializerError
	// -> 之后任何一次访问都是 NoClassDefFoundError: Could not initialize class LithiumConfigAccess。
	// 现在改为惰性求值：只有真正查询 lithium 规则时才去加载配置，并且 ModList 未就绪时不做判定、也不缓存结果。
	@Nullable
	private static volatile Object configCache;
	private static volatile boolean configLoaded;

	private static boolean isModListReady()
	{
		return ModList.get() != null;
	}

	private static boolean isLithiumLoaded()
	{
		ModList modList = ModList.get();
		return modList != null && modList.isLoaded(ModIds.lithium);
	}

	/**
	 * @return lithium 的配置对象；lithium 未加载或 ModList 尚未就绪时返回 null
	 */
	@Nullable
	private static Object getConfig()
	{
		if (configLoaded)
		{
			return configCache;
		}
		if (!isModListReady())
		{
			// Mixin PREPARE 阶段：ModList 还没建好，无法判定。本次返回 null，且不写缓存，
			// 等运行期 ModList 就绪后再正式求值一次。
			return null;
		}
		synchronized (LithiumConfigAccess.class)
		{
			if (!configLoaded)
			{
				configCache = loadLithiumConfig();
				configLoaded = true;
			}
			return configCache;
		}
	}

	/**
	 * For 1.15.2 lithium, check {@link me.jellysquid.mods.lithium.mixin.LithiumMixinPlugin#setupMixins}
	 * and fill interested rules in the switch statement below
	 *
	 * For 1.16+ lithium, {@link net.caffeinemc.mods.lithium.common.config.LithiumConfig#getEffectiveOptionForMixin} exists
	 * so no more manual maintaining
	 */
	public static boolean isLithiumMixinRuleEnabled(String mixinRule)
	{
		Object config = getConfig();
		if (config == null)
		{
			return false;
		}

		if (mixinRule.startsWith("mixin."))
		{
			mixinRule = mixinRule.substring("mixin.".length());
		}
		// in lithium's usage, the passed argument is something like "block.stone.StoneOptimizationMixin"
		// so we need to attach a fake class name as suffix, to make sure lithium's logic works correctly
		Option option = ((LithiumConfig)config).getEffectiveOptionForMixin(mixinRule + ".TISCM_DummyMixin");
		return option != null && option.isEnabled();
	}

	@Nullable
	private static Object loadLithiumConfig()
	{
		if (!isLithiumLoaded())
		{
			return null;
		}

		try
		{
			return LithiumConfig.load(new File("./config/lithium.properties"));
		}
		catch (Exception e)
		{
			CarpetTISAdditionMod.LOGGER.error("Failed to load lithium config", e);
			return null;
		}
	}
}
