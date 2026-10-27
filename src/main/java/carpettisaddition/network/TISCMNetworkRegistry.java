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

package carpettisaddition.network;

import carpettisaddition.CarpetTISAdditionMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * B-S6-2（S6 客户端冒烟时暴露）：NeoForge 的网络层在 `ServerCommonPacketListenerImpl#handleCustomPayload`
 * 里对 modded payload 做"已注册通道"白名单校验（javap 取证见 MIXIN-LOG.md §12.2）：
 * 未在白名单内的 payload 会直接 disconnect（"Incompatible client! ... No Channel for tiscm:network/v1"），
 * 且校验通过后 vanilla 路径会 return，不再走我们的 mixin 分发。
 *
 * 因此这里只用 `RegisterPayloadHandlersEvent` 把 `tiscm:network/v1` 双向登记进 NeoForge 的通道表，
 * handler 故意留空：真正的分发仍由既有的
 * `mixins/network/ServerPlayNetworkHandlerMixin` / `ClientPlayNetworkHandlerMixin` 在 HEAD 处完成，
 * 若这里再加一份分发就会重复处理同一个包。
 */
@EventBusSubscriber(modid = CarpetTISAdditionMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class TISCMNetworkRegistry
{
	private TISCMNetworkRegistry()
	{
	}

	@SubscribeEvent
	public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event)
	{
		PayloadRegistrar registrar = event.registrar("1");
		registrar.playBidirectional(
				TISCMCustomPayload.KEY,
				TISCMCustomPayload.CODEC,
				(payload, context) ->
				{
					// intentionally empty: dispatch happens in the network-handler mixins
				}
		);
		CarpetTISAdditionMod.LOGGER.info("[TISCM] registered payload channel {}", TISCMProtocol.CHANNEL);
	}
}
