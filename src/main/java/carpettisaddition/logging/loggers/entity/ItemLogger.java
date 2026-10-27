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

package carpettisaddition.logging.loggers.entity;

import carpettisaddition.logging.TISAdditionLoggerRegistry;
import carpettisaddition.utils.Messenger;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.network.chat.MutableComponent;

public class ItemLogger extends EntityLogger<ItemEntity>
{
	private static final ItemLogger INSTANCE = new ItemLogger();

	public ItemLogger()
	{
		super("item");
	}

	public static ItemLogger getInstance()
	{
		return INSTANCE;
	}

	@Override
	protected MutableComponent getNameText(ItemEntity item)
	{
		MutableComponent text = super.getNameText(item);
		text.append("(").append(
				Messenger.tr(item.getItem().getDescriptionId())
		).append(")");
		return text;
	}

	@Override
	protected MutableComponent getNameTextHoverText(ItemEntity item)
	{
		return Messenger.c(tr("item_stack_size"), String.format("w : %d", item.getItem().getCount()));
	}

	@Override
	protected boolean getAcceleratorBoolean()
	{
		return TISAdditionLoggerRegistry.__item;
	}
}
