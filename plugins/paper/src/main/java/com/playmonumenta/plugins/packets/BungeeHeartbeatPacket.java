package com.playmonumenta.plugins.packets;

import com.playmonumenta.plugins.Plugin;

public class BungeeHeartbeatPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Bungee.Heartbeat";

	public BungeeHeartbeatPacket() {
		super(null, PacketOperation, null);
	}

	public static void handlePacket(Plugin plugin, BasePacket packet) throws Exception {
		/**TODO: Handle heartbeat
		 * In the future, we'll keep track of how many packets have been sent/received
		 * The response to the heartbeat will be the latest packet the server has received from us
		 * That number packet and all prior to it can be safely deleted from memory.
		 * Any packets later than this need to be resent.
		 *
		 * Currently, the only purpose of the heartbeat is to keep the connection alive, and detect
		 * if/when the connection closes unexpectedly so it can be reopened.
		 */
	}
}
