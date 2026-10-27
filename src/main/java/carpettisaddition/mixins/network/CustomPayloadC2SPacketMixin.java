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

package carpettisaddition.mixins.network;

import carpettisaddition.network.TISCMCustomPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(ServerboundCustomPayloadPacket.class)
public abstract class CustomPayloadC2SPacketMixin
{
	// TIS-S6 (B-S5-1): 1.21.1 里 CustomPacketPayload#codec 多了 ConnectionProtocol + PacketFlow 两个参数
	// （见 MIXIN-LOG.md §12 的 javap 取证），旧描述符的 INVOKE 在 <clinit> 里已不存在，注入 0 候选。
	// 目标改成真实现描述符；<clinit> 里对该方法有两处调用（PLAY / CONFIGURATION），都会被修饰，
	// 于是 STREAM_CODEC 与 CONFIG_STREAM_CODEC 都带上了 TISCM 的 payload。
	// 参数列表在该调用里本来就有且只有一个 List（第 2 个参数），故不需要显式 index。
	@ModifyArg(
			method = "<clinit>",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;codec(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$FallbackProvider;Ljava/util/List;Lnet/minecraft/network/ConnectionProtocol;Lnet/minecraft/network/protocol/PacketFlow;)Lnet/minecraft/network/codec/StreamCodec;"
			)
	)
	private static List<?> registerTISCMPayload_c2s(List<CustomPacketPayload.TypeAndCodec<?, ?>> types)
	{
		types = new ArrayList<>(types);
		types.add(new CustomPacketPayload.TypeAndCodec<>(TISCMCustomPayload.KEY, TISCMCustomPayload.CODEC));
		return Collections.unmodifiableList(types);
	}
}
