package com.playmonumenta.bungeecord.packets;

import java.util.UUID;

import com.google.gson.JsonObject;
import com.playmonumenta.bungeecord.Main;

import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BungeeSendPlayerPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Bungee.SendPlayer";

	public BungeeSendPlayerPacket() throws Exception {
		super();
	}

	public static void handlePacket(Main main, String source, JsonObject data) throws Exception {
		// Message contains just player name, destination server, and player's UUID

		if (!data.has("newServer") ||
		    !data.get("newServer").isJsonPrimitive() ||
		    !data.getAsJsonPrimitive("newServer").isString()) {
			throw new Exception("BungeeGetServerList missing required field 'newServer'");
		}
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

		String newServer = data.get("newServer").getAsString();
		String playerName = data.get("playerName").getAsString();
		UUID playerUUID = UUID.fromString(data.get("playerUUID").getAsString());

		if (newServer == null || playerName == null || playerUUID == null || newServer.length() <= 0 || playerName.length() <= 0) {
			main.getLogger().warning("Got transfer message from '" + source + "' with invalid arguments");
			return;
		}

		// Get and validate the destination server
		ProxyServer proxy = ProxyServer.getInstance();
		ServerInfo serverInfo = proxy.getServers().get(newServer);
		if (serverInfo == null) {
			main.getLogger().warning("Cannot transfer player from '" + source + "' to unknown destination '" + newServer + "'");
			return;
		}

		// Get and validate the player
		ProxiedPlayer playerInfo = proxy.getPlayer(playerUUID);
		if (playerInfo == null) {
			main.getLogger().warning("Cannot transfer unknown player '" + playerName + "' from '" + source + "' to '" + newServer + "'");
			return;
		}

		// Finally transfer the player
		playerInfo.connect(serverInfo, new Callback<Boolean>() {
			@Override
			public void done(Boolean arg0, Throwable arg1) {
				main.getLogger().info("Transferred '" + playerName + "' to '" + newServer + "'");
			}
		});

	}
}
