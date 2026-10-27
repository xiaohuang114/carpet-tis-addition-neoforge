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

package carpettisaddition.utils;

import carpettisaddition.mixins.utils.ServerCommandSourceAccessor;
import carpettisaddition.utils.command.BuilderFactory;
import carpettisaddition.utils.command.CommandSourceStackWithExtraContextArguments;
import com.mojang.brigadier.SingleRedirectModifier;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Optional;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

public class CommandUtils
{
	public static CommandSourceStack getPlayerCommandSource(ServerPlayer serverPlayer)
	{
		// remap loves to remap createCommandSourceStack() to createCommandSourceStackForNameResolution() from mc1.21.1 to mc1.21.3, very stupid
		return serverPlayer.createCommandSourceStack();
	}

	public static boolean isConsoleCommandSource(CommandSourceStack commandSource)
	{
		if (commandSource != null)
		{
			CommandSource output = ((ServerCommandSourceAccessor)commandSource).getOutput();
			return output instanceof MinecraftServer;
		}
		return false;
	}

	public static Optional<ServerPlayer> getPlayer(CommandSourceStack source)
	{
		if (source != null)
		{
			try
			{
				return Optional.ofNullable(source.getPlayerOrException());
			}
			catch (CommandSyntaxException ignored)
			{
			}
		}
		return Optional.empty();
	}

	public static boolean isPlayerCommandSource(CommandSourceStack source)
	{
		return getPlayer(source).isPresent();
	}

	public static boolean isCreativePlayer(CommandSourceStack source)
	{
		return getPlayer(source).
				map(ServerPlayer::isCreative).
				orElse(false);
	}

	public static boolean isOperatorPlayer(CommandSourceStack source)
	{
		return getPlayer(source).
				map(player -> PlayerUtils.isOperator(source.getServer(), player)).
				orElse(false);
	}

	public static boolean isSinglePlayerOwner(CommandSourceStack source)
	{
		MinecraftServer server = source.getServer();
		return getPlayer(source).
				map(player -> server.isSingleplayerOwner(player.getGameProfile())).
				orElse(false);
	}

	/**
	 * aka source has permission level 2 (commonly used in cheaty commands)
	 */
	public static boolean canCheat(CommandSourceStack source)
	{
		return hasPermissionLevel(source, 2);
	}

	public static boolean hasPermissionLevel(CommandSourceStack source, int level)
	{
		return source.hasPermission(level);
	}

	public static SingleRedirectModifier<CommandSourceStack> preservingRedirect()
	{
		return ctx -> {
			CommandSourceStackWithExtraContextArguments sourceExtra = (CommandSourceStackWithExtraContextArguments)ctx.getSource();
			return sourceExtra.createMerge$TISCM(ctx.getSource(), ctx);
		};
	}

	public static SingleRedirectModifier<CommandSourceStack> preservingRedirectWithArg(String name, Object value)
	{
		return ctx -> {
			CommandSourceStackWithExtraContextArguments sourceExtra = (CommandSourceStackWithExtraContextArguments)ctx.getSource();
			sourceExtra.putExtraContextArgument$TISCM(name, new ParsedArgument<>(0, 0, value));
			return sourceExtra.createMerge$TISCM(ctx.getSource(), ctx);
		};
	}

	@FunctionalInterface
	public interface ArgumentGetter<T>
	{
		T get() throws CommandSyntaxException;
	}

	/**
	 * Suppressed the {@link IllegalArgumentException} in {@link com.mojang.brigadier.context.CommandContext#getArgument}
	 * But {@link CommandSyntaxException} is allowed
	 */
	public static <T> Optional<T> getOptArg(ArgumentGetter<T> argGetter) throws CommandSyntaxException
	{
		try
		{
			return Optional.of(argGetter.get());
		}
		catch (IllegalArgumentException e)
		{
			return Optional.empty();
		}
	}

	public static RequiredArgumentBuilder<CommandSourceStack, String> enumArg(String name, Class<? extends Enum<?>> enumClass)
	{
		return argument(name, word()).
				suggests((c, b) -> suggest(
						Arrays.stream(enumClass.getEnumConstants()).
								map(e -> e.name().toLowerCase()),
						b)
				);
	}

	public static BuilderFactory<RequiredArgumentBuilder<CommandSourceStack, String>> enumArg(Class<? extends Enum<?>> enumClass)
	{
		return name -> enumArg(name, enumClass);
	}

	/**
	 * @return the corresponding enum, or Optional.empty() on invalid enum
	 * @throws IllegalArgumentException on arg not found
	 */
	public static <T extends Enum<T>> Either<T, String> getEnum(CommandContext<?> context, String argName, Class<T> enumClass)
	{
		String enumString = getString(context, argName);
		try
		{
			return Either.left(Enum.valueOf(enumClass, enumString.toUpperCase()));
		}
		catch (IllegalArgumentException e)
		{
			return Either.right(enumString);
		}
	}
}
