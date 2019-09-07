package com.playmonumenta.bungeecord.packets;

import com.google.gson.JsonObject;
import com.playmonumenta.bungeecord.network.ClientSocket;
import com.playmonumenta.bungeecord.network.SocketManager;

/**
 * CommandPacket sends a command to a single shard.
 */
public class ShardCommandPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Shard.Command";
	/**
	 * Create a new packet containing a command.
	 * This command will be sent to a single shard
	 * @param target
	 * @param command
	 */
	public ShardCommandPacket(String targetShard, String command) {
		super(targetShard, PacketOperation, new JsonObject());
		mData.addProperty("command", command);
	}

	public static void handlePacket(SocketManager manager, ClientSocket client, BasePacket packet) throws Exception {
		throw new Exception("ShardCommandPacket cannot be handled by bungee");
	}
}
