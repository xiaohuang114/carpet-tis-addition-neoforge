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

package carpettisaddition.commands.speedtest.skipcompression;

import carpettisaddition.commands.speedtest.SpeedTestPacketUtils;
import carpettisaddition.network.TISCMProtocol;
import carpettisaddition.utils.NetworkUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.Util;

import java.util.Arrays;
import java.util.Objects;
import carpettisaddition.mixins.command.speedtest.PacketCodecDispatcherAccessor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.CommonPacketTypes;
import net.minecraft.network.protocol.game.GameProtocols;

public class SpeedTestCompressionSkipper
{
	private static final byte[] DOWNLOAD_PACKET_BYTES_PREFIX = Util.make(() -> {
		TISCMProtocol.S2C packetId = TISCMProtocol.S2C.SPEED_TEST_DOWNLOAD_PAYLOAD;
		return makeBytes(getMinecraftPacketId(PacketFlow.CLIENTBOUND, packetId.packet(nbt -> {})), packetId.getId());
	});

	private static final byte[] UPLOAD_PACKET_BYTES_PREFIX = Util.make(() -> {
		TISCMProtocol.C2S packetId = TISCMProtocol.C2S.SPEED_TEST_UPLOAD_PAYLOAD;
		Packet<?> examplePacket =
				packetId.packet(nbt -> {});
		return makeBytes(getMinecraftPacketId(PacketFlow.SERVERBOUND, examplePacket), packetId.getId());
	});

	private static byte[] makeBytes(int mcPacketId, String tiscmPacketId)
	{
		FriendlyByteBuf buf = NetworkUtils.newFriendlyByteBuf();
		buf.writeVarInt(mcPacketId);
		buf.writeResourceLocation(TISCMProtocol.CHANNEL);
		buf.writeUtf(tiscmPacketId);

		byte[] result = new byte[buf.readableBytes()];
		buf.readBytes(result);
		buf.release();
		return result;
	}

	private static int getMinecraftPacketId(PacketFlow side, Packet<?> packet)
	{
		try
		{
			// side == server (SERVERBOUND) means receiving c2s packet
			var factory = side == PacketFlow.SERVERBOUND ? GameProtocols.SERVERBOUND_TEMPLATE : GameProtocols.CLIENTBOUND_TEMPLATE;
			var codec = factory.bind(buf -> new RegistryFriendlyByteBuf(buf, null)).codec();
			var type = side == PacketFlow.SERVERBOUND ? CommonPacketTypes.SERVERBOUND_CUSTOM_PAYLOAD : CommonPacketTypes.CLIENTBOUND_CUSTOM_PAYLOAD;
			if (codec instanceof PacketCodecDispatcherAccessor<?> packetCodecDispatcher)
			{
				var packetId = packetCodecDispatcher.getTypeToIndex().getOrDefault(type, -1);
				if (packetId == -1)
				{
					throw new IndexOutOfBoundsException("Failed to get packet id for %s from %s".formatted(type, packetCodecDispatcher));
				}
				return packetId;
			}
			else
			{
				throw new RuntimeException("codec is not a PacketCodecDispatcherAccessor");
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException("failed to get " + side + " packet id of " + packet);
		}
	}

	public static boolean isSpeedTestPayloadPacket(PacketDeflaterWithNetworkSide deflater, ByteBuf byteBuf)
	{
		PacketFlow side = deflater.getNetworkSide$TISCM();
		if (side == null)
		{
			return false;
		}

		byte[] prefix;
		switch (side)
		{
			case SERVERBOUND:  // I'm the server, deflate for S->C, download
				prefix = DOWNLOAD_PACKET_BYTES_PREFIX;
				break;
			case CLIENTBOUND:  // I'm the client, deflate for C->S, upload
				prefix = UPLOAD_PACKET_BYTES_PREFIX;
				break;
			default:
				return false;
		}

		byteBuf = byteBuf.slice();
		int n = byteBuf.readableBytes();
		if (n >= prefix.length && n >= SpeedTestPacketUtils.SIZE_PER_PACKET - 200)
		{
			byte[] sample = new byte[prefix.length];
			byteBuf.readBytes(sample);
			return Arrays.equals(sample, prefix);
		}

		return false;
	}
}
