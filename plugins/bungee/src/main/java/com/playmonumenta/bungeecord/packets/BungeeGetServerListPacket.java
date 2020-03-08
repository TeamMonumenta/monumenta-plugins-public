package com.playmonumenta.bungeecord.packets;

import java.util.UUID;

import com.google.gson.JsonObject;
import com.playmonumenta.bungeecord.Main;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BungeeGetServerListPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Bungee.GetServerList";

	public BungeeGetServerListPacket() throws Exception {
		super();
	}

	public static void handlePacket(Main main, String source, JsonObject data) throws Exception {
		// Message contains just player name and player's UUID
		if (!data.has("playerName") ||
		    !data.get("playerName").isJsonPrimitive() ||
		    !data.getAsJsonPrimitive("playerName").isString()) {
			throw new Exception("BungeeGetServerList missing required field 'playerName'");
		}
		if (!data.has("playerUUID") ||
		    !data.get("playerUUID").isJsonPrimitive() ||
		    !data.getAsJsonPrimitive("playerUUID").isString()) {
			throw new Exception("BungeeGetServerList missing required field 'playerUUID'");
		}
		String player = data.get("playerName").getAsString();
		UUID uuid = UUID.fromString(data.get("playerUUID").getAsString());
		ProxiedPlayer playerInfo = main.getProxy().getPlayer(uuid);

		// Check arguments
		if (player == null || uuid == null || player.length() <= 0 || playerInfo == null) {
			main.getLogger().warning("Got GetServerList command from '" + source + "' with invalid arguments");
			return;
		}

		// Print list of servers to player
		TextComponent header = new TextComponent("Available Monumenta servers:");
		header.setColor(ChatColor.GOLD);
		playerInfo.sendMessage(header);
		/* TODO re-implement this using kubernetes at some point */
		TextComponent serverMessage = new TextComponent("  TODO: Not Implemented!");
		serverMessage.setColor(ChatColor.GOLD);
		playerInfo.sendMessage(serverMessage);
	}
}
