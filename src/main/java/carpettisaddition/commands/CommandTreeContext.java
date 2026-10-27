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

package carpettisaddition.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandBuildContext;

public abstract class CommandTreeContext
{
	public final CommandBuildContext commandBuildContext;

	/*
	 * ---------------------
	 *       Factories
	 * ---------------------
	 */

	protected CommandTreeContext(
			CommandBuildContext commandBuildContext
	)
	{
		this.commandBuildContext = commandBuildContext;
	}

	public static Register of(
			CommandDispatcher<CommandSourceStack> dispatcher
			, CommandBuildContext commandBuildContext
	)
	{
		return new Register(
				dispatcher
				, commandBuildContext
		);
	}

	public static Node of(
			ArgumentBuilder<CommandSourceStack, ?> node
			, CommandBuildContext commandBuildContext
	)
	{
		return new Node(
				node
				, commandBuildContext
		);
	}

	/**
	 * For mc1.19+
	 * Warning: {@link #commandBuildContext} will be null
	 * <p>
	 * TODO: make an interface with getCommandBuildContext() method.
	 * then make this method returns a impl that getCommandBuildContext() throws
	 */
	public static Node ofNonContext(ArgumentBuilder<CommandSourceStack, ?> node)
	{
		return of(
				node
				, null
		);
	}

	/*
	 * ---------------------
	 *      Convertors
	 * ---------------------
	 */

	/**
	 * Creates a {@link Node} context based on self's basic information
	 */
	public Node node(ArgumentBuilder<CommandSourceStack, ?> node)
	{
		return of(
				node
				, commandBuildContext
		);
	}

	public static class Register extends CommandTreeContext
	{
		public final CommandDispatcher<CommandSourceStack> dispatcher;

		private Register(
				CommandDispatcher<CommandSourceStack> dispatcher
				, CommandBuildContext commandBuildContext
		)
		{
			super(
					commandBuildContext
			);
			this.dispatcher = dispatcher;
		}
	}

	/*
	 * ---------------------
	 * Detailed sub-classes
	 * ---------------------
	 */

	public static class Node extends CommandTreeContext
	{
		public final ArgumentBuilder<CommandSourceStack, ?> node;

		private Node(
				ArgumentBuilder<CommandSourceStack, ?> node
				, CommandBuildContext commandBuildContext
		)
		{
			super(
					commandBuildContext
			);
			this.node = node;
		}
	}
}
