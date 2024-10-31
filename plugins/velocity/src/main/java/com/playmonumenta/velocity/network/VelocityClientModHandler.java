package com.playmonumenta.velocity.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.playmonumenta.velocity.MonumentaVelocity;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.nio.charset.StandardCharsets;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class VelocityClientModHandler {
	public static final ChannelIdentifier CHANNEL_ID = MinecraftChannelIdentifier.create("monumenta", "client_channel_v1");

	private static @MonotonicNonNull VelocityClientModHandler INSTANCE = null;

	private final Gson mGson;

	public static boolean mAllowPublicizeContent;

	public VelocityClientModHandler(MonumentaVelocity plugin, boolean allowPublicizeContent) {
		mAllowPublicizeContent = allowPublicizeContent;
		mGson = new GsonBuilder().create();
		INSTANCE = this;
	}

	public static void sendServerInfoPacket(Player player) {
		VelocityClientModHandler.ServerInfoPacket serverInfoPacket = new VelocityClientModHandler.ServerInfoPacket();
		serverInfoPacket.allowPublicizeContent = mAllowPublicizeContent;
		INSTANCE.sendPacket(player, serverInfoPacket);
	}

	/**
	 * sent on login, gives information that the client should know first and foremost.
	 */
	@SuppressWarnings("unused")
	public static class ServerInfoPacket implements Packet {
		String _type = "ServerInfoPacket";

		/**
		 * tells the clientside mod whether it is allowed to release information on the location of the player.
		 * example: beta testing of new content. Should be disabled on Stage and Volt.
		 */
		boolean allowPublicizeContent;
	}

	interface Packet {
	}

	private void sendPacket(Player player, Packet packet) {
		player.sendPluginMessage(CHANNEL_ID, mGson.toJson(packet).getBytes(StandardCharsets.UTF_8));
	}
}
