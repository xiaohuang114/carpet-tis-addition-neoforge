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

package carpettisaddition.mixins.carpet.hooks;

import carpet.logging.HUDController;
import carpettisaddition.logging.TISAdditionHUDController;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import java.util.List;
import java.util.function.Consumer;

/**
 * Using mixin instead of directly invoking HUDController#register should have better carpet version compatibility,
 * since HUDController#register is not a static method, which doesn't seem to be intentional and might gets changed
 * in the future version
 */
@Mixin(HUDController.class)
public abstract class HUDControllerMixin
{

	@Shadow(remap = false)
	private static List<Consumer<MinecraftServer>> HUDListeners;

	static
	{
		HUDListeners.add(TISAdditionHUDController::updateHUD);
	}
}
