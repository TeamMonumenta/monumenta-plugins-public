package com.playmonumenta.bungeecord.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class BungeeClientModHandler {
	public static final String CHANNEL_ID = "monumenta:client_channel_v1";

	private static @MonotonicNonNull BungeeClientModHandler INSTANCE = null;

	private final Gson mGson;
	private final Logger mLogger;

	public static boolean mAllowPublicizeContent;

	public BungeeClientModHandler(boolean allowPublicizeContent, Logger logger) {
		mAllowPublicizeContent = allowPublicizeContent;
		mGson = new GsonBuilder().create();
		mLogger = logger;
		INSTANCE = this;
	}

	public static void sendServerInfoPacket(ProxiedPlayer player) {
		ServerInfoPacket serverInfoPacket = new ServerInfoPacket();
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

	private void sendPacket(ProxiedPlayer player, Packet packet) {
		mLogger.fine("sent packet: " + packet.getClass().getSimpleName() + " to " + player.getName());
		player.sendData(CHANNEL_ID, mGson.toJson(packet).getBytes(StandardCharsets.UTF_8));
	}
}
