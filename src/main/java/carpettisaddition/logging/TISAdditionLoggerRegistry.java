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

package carpettisaddition.logging;
import carpet.logging.LoggerRegistry;

import carpet.logging.HUDLogger;
import carpet.logging.Logger;
import carpettisaddition.CarpetTISAdditionMod;
import carpettisaddition.logging.loggers.AbstractLogger;
import carpettisaddition.logging.loggers.commandblock.CommandBlockLogger;
import carpettisaddition.logging.loggers.damage.DamageLogger;
import carpettisaddition.logging.loggers.entity.ItemLogger;
import carpettisaddition.logging.loggers.entity.XPOrbLogger;
import carpettisaddition.logging.loggers.entityIdCounter.EntityIdCounterHUDLogger;
import carpettisaddition.logging.loggers.lifetime.LifeTimeHUDLogger;
import carpettisaddition.logging.loggers.lightqueue.LightQueueHUDLogger;
import carpettisaddition.logging.loggers.memory.MemoryHUDLogger;
import carpettisaddition.logging.loggers.microtiming.utils.MicroTimingStandardCarpetLogger;
import carpettisaddition.logging.loggers.movement.MovementLogger;
import carpettisaddition.logging.loggers.phantom.PhantomLogger;
import carpettisaddition.logging.loggers.portalCreation.PortalCreationLogger;
import carpettisaddition.logging.loggers.raid.RaidLogger;
import carpettisaddition.logging.loggers.scounter.SupplierCounterHUDLogger;
import carpettisaddition.logging.loggers.ticket.TicketLogger;
import carpettisaddition.logging.loggers.tickwarp.TickWarpHUDLogger;
import carpettisaddition.logging.loggers.turtleegg.TurtleEggLogger;
import carpettisaddition.logging.loggers.wanderingTrader.WanderingTraderLogger;
import carpettisaddition.logging.loggers.xcounter.XpCounterHUDLogger;
import com.google.common.collect.Lists;

import java.lang.reflect.Field;
import java.util.List;
import carpettisaddition.logging.loggers.mobcapsLocal.MobcapsLocalLogger;

public class TISAdditionLoggerRegistry
{
	private static final List<Runnable> onRegisteredCallbacks = Lists.newArrayList();

	public static boolean __commandBlock;
	public static boolean __entityIdCounter;
	public static boolean __damage;
	public static boolean __item;
	public static boolean __lifetime;
	public static boolean __lightQueue;
	public static boolean __memory;
	public static boolean __microTiming;
	public static boolean __mobcapsLocal;
	public static boolean __movement;
	public static boolean __phantom;
	public static boolean __portalCreation;
	public static boolean __raid;
	public static boolean __scounter;
	public static boolean __ticket;
	public static boolean __tickWarp;
	public static boolean __turtleEgg;
	public static boolean __wanderingTrader;
	public static boolean __xporb;
	public static boolean __xcounter;

	public static void registerLoggers()
	{
		register(CommandBlockLogger.getInstance());
		register(DamageLogger.getInstance());
		register(EntityIdCounterHUDLogger.getInstance());
		register(ItemLogger.getInstance());
		register(LifeTimeHUDLogger.getInstance());
		register(LightQueueHUDLogger.getInstance());
		register(MemoryHUDLogger.getInstance());
		register(MicroTimingStandardCarpetLogger.getInstance());
		register(MobcapsLocalLogger.getInstance());
		register(MovementLogger.getInstance());
		register(PhantomLogger.getInstance());
		register(PortalCreationLogger.getInstance());
		register(RaidLogger.getInstance());
		register(SupplierCounterHUDLogger.getInstance());
		register(TicketLogger.getInstance());
		register(TickWarpHUDLogger.getInstance());
		register(TurtleEggLogger.getInstance());
		register(WanderingTraderLogger.getInstance());
		register(XpCounterHUDLogger.getInstance());
		register(XPOrbLogger.getInstance());

		onRegisteredCallbacks.forEach(Runnable::run);
	}

	private static void register(AbstractLogger logger)
	{
		register(logger.createCarpetLogger());
	}

	private static void register(Logger logger)
	{
		LoggerRegistry.registerLogger(logger.getLogName(), logger);
	}

	public static Field getLoggerField(String logName)
	{
		try
		{
			return TISAdditionLoggerRegistry.class.getField("__" + logName);
		}
		catch (NoSuchFieldException e)
		{
			throw new RuntimeException(String.format("Failed to get logger field \"%s\" @ %s", logName, CarpetTISAdditionMod.MOD_NAME));
		}
	}

	public static Logger standardLogger(
			String logName, String def, String[] options
			, boolean strictOptions
	)
	{
		return new
				Logger
				(
						getLoggerField(logName), logName, def, options
						, strictOptions
				);
	}

	public static HUDLogger standardHUDLogger(
			String logName, String def, String [] options
			, boolean strictOptions
	)
	{
		return new
				HUDLogger
				(
						getLoggerField(logName), logName, def, options
						, strictOptions
				);
	}

	public static void addLoggerRegisteredCallback(Runnable callback)
	{
		onRegisteredCallbacks.add(callback);
	}
}
