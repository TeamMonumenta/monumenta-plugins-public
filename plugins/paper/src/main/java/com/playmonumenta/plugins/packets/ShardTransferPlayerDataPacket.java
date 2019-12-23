package com.playmonumenta.plugins.packets;

import java.util.UUID;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.player.PlayerData;
import com.playmonumenta.plugins.utils.NetworkUtils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class ShardTransferPlayerDataPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Shard.TransferPlayerData";

	public ShardTransferPlayerDataPacket(String newServer, String playerName, UUID playerUUID, String playerContent) {
		super(newServer, PacketOperation, new JsonObject());
		mData.addProperty("newServer", newServer);
		mData.addProperty("playerName", playerName);
		mData.addProperty("playerUUID", playerUUID.toString());
		mData.addProperty("playerContent", playerContent);
	}

	public static void handlePacket(Plugin plugin, BasePacket packet) throws Exception {
		if (!packet.hasData() ||
		    !packet.getData().has("newServer") ||
		    !packet.getData().get("newServer").isJsonPrimitive() ||
		    !packet.getData().getAsJsonPrimitive("newServer").isString()) {
			throw new Exception("ShardTransferPlayerDataPacket missing required field 'newServer'");
		}
		if (!packet.getData().has("playerName") ||
		    !packet.getData().get("playerName").isJsonPrimitive() ||
		    !packet.getData().getAsJsonPrimitive("playerName").isString()) {
			throw new Exception("ShardTransferPlayerDataPacket missing required field 'playerName'");
		}
		if (!packet.getData().has("playerUUID") ||
		    !packet.getData().get("playerUUID").isJsonPrimitive() ||
		    !packet.getData().getAsJsonPrimitive("playerUUID").isString()) {
			throw new Exception("ShardTransferPlayerDataPacket missing required field 'playerUUID'");
		}
		if (!packet.getData().has("playerContent") ||
		    !packet.getData().get("playerContent").isJsonPrimitive() ||
		    !packet.getData().getAsJsonPrimitive("playerContent").isString()) {
			throw new Exception("ShardTransferPlayerDataPacket missing required field 'playerContent'");
		}

		String newServer = packet.getData().get("newServer").getAsString();
		String playerName = packet.getData().get("playerName").getAsString();
		UUID playerUUID = UUID.fromString(packet.getData().get("playerUUID").getAsString());
		String playerContent = packet.getData().get("playerContent").getAsString();

		// Save the player data so that when the player logs in they'll get it applied to them
		PlayerData.savePlayerData(plugin, playerUUID, playerContent);

		// Everything looks good - request bungeecord transfer the player to this server
		NetworkUtils.sendPlayer(plugin, playerName, playerUUID, newServer);
	}

	public static void handleError(Plugin plugin, BasePacket packet) throws Exception {
		// Failed to transfer the player to the requested server
		// Notify player and unfreeze their inventory
		if (!packet.hasData() ||
		    !packet.getData().has("newServer") ||
		    !packet.getData().get("newServer").isJsonPrimitive() ||
		    !packet.getData().getAsJsonPrimitive("newServer").isString()) {
			throw new Exception("ShardTransferPlayerDataPacketError missing required field 'newServer'");
		}
		if (!packet.getData().has("playerUUID") ||
		    !packet.getData().get("playerUUID").isJsonPrimitive() ||
		    !packet.getData().getAsJsonPrimitive("playerUUID").isString()) {
			throw new Exception("ShardTransferPlayerDataPacketError missing required field 'playerUUID'");
		}

		String newServer = packet.getData().get("newServer").getAsString();
		UUID playerUUID = UUID.fromString(packet.getData().get("playerUUID").getAsString());

		Player player = plugin.getPlayer(playerUUID);
		if (player != null) {
			player.sendMessage(ChatColor.RED + "Bungee reports server '" + newServer + "' is not available!");

			/* Call this on the main thread */
			// Remove the metadata that prevents player from interacting with things (if present)
			Bukkit.getScheduler().callSyncMethod(plugin,
																						() -> {
																								player.removeMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY, plugin);
																								return true; //Does nothing - conforms to functional interface
																						}
			);
		}
	}
}
