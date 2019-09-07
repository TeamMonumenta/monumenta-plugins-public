package com.playmonumenta.bungeecord.packets;

import java.util.UUID;

import com.google.gson.JsonObject;
import com.playmonumenta.bungeecord.network.ClientSocket;
import com.playmonumenta.bungeecord.network.SocketManager;

import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BungeeSendPlayerPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Bungee.SendPlayer";

	public BungeeSendPlayerPacket(String newServer, String playerName, UUID playerUUID) {
		super(null, PacketOperation, new JsonObject());
		mData.addProperty("newServer", newServer);
		mData.addProperty("playerName", playerName);
		mData.addProperty("playerUUID", playerUUID.toString());
	}

	public static void handlePacket(SocketManager manager, ClientSocket client, BasePacket packet) throws Exception {
		// Message contains just player name, destination server, and player's UUID

		if (!packet.hasData() ||
		    !packet.getData().has("newServer") ||
		    !packet.getData().get("newServer").isJsonPrimitive() ||
		    !packet.getData().getAsJsonPrimitive("newServer").isString()) {
			throw new Exception("BungeeGetServerList missing required field 'newServer'");
		}
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

		String newServer = packet.getData().get("newServer").getAsString();
		String playerName = packet.getData().get("playerName").getAsString();
		UUID playerUUID = UUID.fromString(packet.getData().get("playerUUID").getAsString());

		if (newServer == null || playerName == null || playerUUID == null || newServer.length() <= 0 || playerName.length() <= 0) {
			manager.mMain.getLogger().warning("Got transfer message from '" + client.getName() + "' with invalid arguments");
			return;
		}

		// Get and validate the destination server
		ServerInfo serverInfo = manager.mProxy.getServers().get(newServer);
		if (serverInfo == null) {
			manager.mMain.getLogger().warning("Cannot transfer player from '" + client.getName() + "' to unknown destination '" + newServer + "'");
			return;
		}

		// Get and validate the player
		ProxiedPlayer playerInfo = manager.mProxy.getPlayer(playerUUID);
		if (playerInfo == null) {
			manager.mMain.getLogger().warning("Cannot transfer unknown player '" + playerName + "' from '" + client.getName() + "' to '" + newServer + "'");
			return;
		}

		// Finally transfer the player
		playerInfo.connect(serverInfo, new Callback<Boolean>() {
			@Override
			public void done(Boolean arg0, Throwable arg1) {
				manager.mMain.getLogger().info("Transferred '" + playerName + "' to '" + newServer + "'");
			}
		});

	}
}
