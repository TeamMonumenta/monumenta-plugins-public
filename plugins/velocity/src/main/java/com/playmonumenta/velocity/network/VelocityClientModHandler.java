package com.playmonumenta.velocity.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.playmonumenta.velocity.MonumentaVelocity;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChannelRegisterEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class VelocityClientModHandler {
	public static final ChannelIdentifier CHANNEL_ID = MinecraftChannelIdentifier.create("monumenta", "client_channel_v1");

	private static @MonotonicNonNull VelocityClientModHandler INSTANCE = null;

	private final Gson mGson;

	public static boolean mAllowPublicizeContent;

	/* Keeps track of players that have registered the client packet channel (added in PlayerChannelRegisterEvent and removed following DisconnectEvent)*/
	private static final Set<UUID> mOnlinePlayersWithClientChannel = new ConcurrentSkipListSet<UUID>();

	public VelocityClientModHandler(MonumentaVelocity plugin, boolean allowPublicizeContent) {
		mAllowPublicizeContent = allowPublicizeContent;
		mGson = new GsonBuilder().create();
		INSTANCE = this;
	}

	@Subscribe(order = PostOrder.EARLY)
	public void onPlayerChannelRegisterEvent(PlayerChannelRegisterEvent event) {
		if (!event.getChannels().contains(CHANNEL_ID)) {
			return;
		}

		Player player = event.getPlayer();
		if (!mOnlinePlayersWithClientChannel.contains(player.getUniqueId())) {
			sendServerInfoPacket(player);

			mOnlinePlayersWithClientChannel.add(player.getUniqueId());
		}
	}

	public static void onPlayerDisconnected(Player player) {
		mOnlinePlayersWithClientChannel.remove(player.getUniqueId());
	}

	public static void sendServerInfoPacket(Player player) {
		VelocityClientModHandler.ServerInfoPacket serverInfoPacket = new VelocityClientModHandler.ServerInfoPacket();
		serverInfoPacket.allowPublicizeContent = mAllowPublicizeContent;
		INSTANCE.sendPacket(player, serverInfoPacket);
	}

	public static boolean playerHasClientMod(Player player) {
		return mOnlinePlayersWithClientChannel.contains(player.getUniqueId());
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
