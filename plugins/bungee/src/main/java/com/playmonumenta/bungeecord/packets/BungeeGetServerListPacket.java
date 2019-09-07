package com.playmonumenta.bungeecord.packets;

import java.util.UUID;

import com.google.gson.JsonObject;
import com.playmonumenta.bungeecord.Main;
import com.playmonumenta.bungeecord.network.ClientSocket;
import com.playmonumenta.bungeecord.network.SocketManager;
import com.playmonumenta.bungeecord.network.ClientSocket.Status;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BungeeGetServerListPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Bungee.GetServerList";

	public BungeeGetServerListPacket(String playerName, UUID playerUUID) {
		super(null, PacketOperation, new JsonObject());
		mData.addProperty("playerName", playerName);
		mData.addProperty("playerUUID", playerUUID.toString());
	}

	public static void handlePacket(SocketManager manager, ClientSocket client, BasePacket packet) throws Exception {
		// Message contains just player name and player's UUID
		if (!packet.hasData() ||
		    !packet.getData().has("playerName") ||
		    !packet.getData().get("playerName").isJsonPrimitive() ||
		    !packet.getData().getAsJsonPrimitive("playerName").isString()) {
			throw new Exception("BungeeGetServerList missing required field 'playerName'");
		}
		if (!packet.hasData() ||
		    !packet.getData().has("playerUUID") ||
		    !packet.getData().get("playerUUID").isJsonPrimitive() ||
		    !packet.getData().getAsJsonPrimitive("playerUUID").isString()) {
			throw new Exception("BungeeGetServerList missing required field 'playerUUID'");
		}
		String player = packet.getData().get("playerName").getAsString();
		UUID uuid = UUID.fromString(packet.getData().get("playerUUID").getAsString());
		ProxiedPlayer playerInfo = manager.mMain.getProxy().getPlayer(uuid);

		// Check arguments
		if (player == null || uuid == null || player.length() <= 0 || playerInfo == null) {
			manager.mMain.getLogger().warning("Got GetServerList command from '" + client.getName() + "' with invalid arguments");
			return;
		}

		// Print list of servers to player
		TextComponent header = new TextComponent("Available Monumenta servers:");
		header.setColor(ChatColor.GOLD);
		playerInfo.sendMessage(header);
		for (ClientSocket clientSocket : manager.getClients()) {
			if (clientSocket.getStatus() == Status.OPEN) {
				TextComponent serverMessage = new TextComponent("  " + clientSocket.getName());
				serverMessage.setColor(ChatColor.GOLD);
				playerInfo.sendMessage(serverMessage);
			}
		}
	}
}
