/*
 * This file is part of the Carpet TIS Addition project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2025  Fallen_Breath and contributors
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

package carpettisaddition.mixins.carpet.tweaks.command.fakePlayerRejoin;

import carpet.patches.EntityPlayerMPFake;
import carpettisaddition.CarpetTISAdditionMod;
import carpettisaddition.helpers.carpet.tweaks.command.fakePlayerRejoin.FakePlayerRejoinHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import java.util.function.Consumer;

@Mixin(EntityPlayerMPFake.class)
public abstract class EntityPlayerMPFake_ForwardFlagMixin
{
	@ModifyArg(
			method = "createFake",
			at = @At(
					value = "INVOKE",
					target = "Ljava/util/concurrent/CompletableFuture;thenAcceptAsync(Ljava/util/function/Consumer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;",
					remap = false
			),
			remap = true
	)
	private static <T> Consumer<? super T> fakePlayerRejoin_forwardTheFlag(Consumer<? super T> action)
	{
		boolean isRejoin = FakePlayerRejoinHelper.isRejoin.get();
		return value -> {
			if (isRejoin)
			{
				FakePlayerRejoinHelper.isRejoin.set(true);
			}
			try
			{
				action.accept(value);
			}
			catch (Throwable e)
			{
				CarpetTISAdditionMod.LOGGER.error("createFake delayed task error", e);
				throw e;
			}
			finally
			{
				FakePlayerRejoinHelper.isRejoin.remove();
			}
		};
	}
}
