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

package carpettisaddition;

import carpettisaddition.utils.AutoMixinAuditExecutor;
import net.neoforged.fml.ModList;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class CarpetTISAdditionMod
{
	public static final String MOD_ID = "carpet_tis_addition";
	public static final String MOD_NAME = "Carpet TIS Addition";
	public static final String COMPACT_NAME = "carpettisaddition";  // 不派生自 MOD_ID：它决定翻译命名空间与 config 路径
	private static String version;

	public static final Logger LOGGER =
			LogUtils.getLogger();

	/**
	 * 由 NeoForge 入口 {@link carpettisaddition.neoforge.TISNeoForgeMod} 调用。
	 * NeoForge 没有 Fabric 的 ModInitializer，故这里只是一个普通的静态初始化方法。
	 */
	public static void initialize()
	{
		version = ModList.get().getModContainerById(MOD_ID).orElseThrow(RuntimeException::new).getModInfo().getVersion().toString();

		CarpetTISAdditionServer.init();
		AutoMixinAuditExecutor.run();
	}

	public static String getVersion()
	{
		return version;
	}
}
