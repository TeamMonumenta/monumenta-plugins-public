package bungee.project.listeners;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

import bungee.project.Main;
import bungee.project.utils.PacketUtils;
import net.md_5.bungee.api.plugin.Listener;

import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import fr.rhaz.socketapi.SocketAPI.Server.SocketMessenger;
import fr.rhaz.socket4mc.Bungee.BungeeSocketHandshakeEvent;
import fr.rhaz.socket4mc.Bungee.BungeeSocketJSONEvent;

import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class EventListener implements Listener {

	Main mMain;

	// Collection of all connected sockets
	private static HashMap<String, SocketMessenger> mSockets;

	public EventListener(Main main) {
		mMain = main;
		mSockets = new HashMap<>();
	}

	// Every time a bungee server shard connects, add its socket to the list
	@EventHandler(priority = EventPriority.LOWEST)
    public void onHandshake(BungeeSocketHandshakeEvent e) {
        mSockets.put(e.getName(), e.getMessenger());
    }

	@EventHandler(priority = EventPriority.LOWEST)
	public void onMessage(BungeeSocketJSONEvent e) {
		String channel = e.getChannel(); // The channel ("MyBukkitPlugin")
		String sendingServer = e.getName(); // The Spigot server name
		String rawData = e.getData(); // The data the plugin sent you
		// Nothing to do with no payload
		if (rawData == null || rawData.length() <= 0) {
			mMain.getLogger().warning("Got message from '" + sendingServer + "' with empty data");
			return;
		}

		if ((!channel.startsWith("Monumenta.Bungee.Forward."))
			&& (!channel.startsWith("Monumenta.Bungee.Broadcast."))
			&& (!channel.equals("Monumenta.Bungee.SendPlayer"))
			&& (!channel.equals("Monumenta.Bungee.Heartbeat"))) {
			mMain.getLogger().warning("Got message from '" + sendingServer + "' with invalid channel '" + channel + "'");
			return;
		}

		/* Special case - no payload */
		if (channel.equals("Monumenta.Bungee.Heartbeat")) {
			mMain.getLogger().info("Got heartbeat message from '" + sendingServer + "'");
			return;
		}

		/* For all other message types, attempt to decode payload */
		String[] rcvStrings = null;
		try {
			rcvStrings = PacketUtils.decodeStrings(rawData);
		} catch (Exception ex) {
			rcvStrings = null;
		}
		if (rcvStrings == null || rcvStrings.length <= 0) {
			mMain.getLogger().warning("Got message from '" + sendingServer + "' with invalid payload");
			return;
		}

		if (channel.startsWith("Monumenta.Bungee.Forward.")) {
			bungeeForward(channel, sendingServer, rcvStrings, rawData);
		} else if (channel.startsWith("Monumenta.Bungee.Broadcast.")) {
			bungeeBroadcast(channel, sendingServer, rawData);
		} else if (channel.equals("Monumenta.Bungee.SendPlayer")) {
			sendPlayer(sendingServer, rcvStrings);
		} else {
			mMain.getLogger().warning("Got message from '" + sendingServer + "' with unhandled channel '" + channel + "'");
		}
	}

	private void bungeeForward(String channel, String sendingServer, String[] rcvStrings, String rawData) {
		// First component of this type of packet is the destination server
		String destination = rcvStrings[0];

		if (destination == null || (!mSockets.containsKey(destination))) {
			mMain.getLogger().warning("Cannot forward message from '" + sendingServer + "' to unknown destination '" + destination + "'");
			mMain.getLogger().warning("mSockets list contents:");
			for (Map.Entry<String, SocketMessenger> entry : mSockets.entrySet()) {
				mMain.getLogger().warning("  " + entry.getKey());
			}
			return;
		}

		// Look up the destination socket
		SocketMessenger socketDest = mSockets.get(destination);

		if ((!socketDest.isHandshaked()) || (!socketDest.isConnectedAndOpened())) {
			mMain.getLogger().warning("Cannot forward message from '" + sendingServer + "' because '" + destination + "' has not finished connecting");
			return;
		}

		// Finally forward the message
		socketDest.writeJSON(channel, rawData);
		mMain.getLogger().info("Forwarded message from '" + sendingServer + "' to '" + destination + "'");
	}

	// Sends a message to all connected servers (including the one that sent it)
	private void bungeeBroadcast(String channel, String sendingServer, String rawData) {
		mMain.getLogger().info("Broadcasting message from '" + sendingServer + "' on channel '" + channel + "'");
		for (Map.Entry<String, SocketMessenger> entry : mSockets.entrySet()) {
			SocketMessenger socketDest = entry.getValue();

			if ((!socketDest.isHandshaked()) || (!socketDest.isConnectedAndOpened())) {
				mMain.getLogger().warning("Cannot send broadcast message to '" + entry.getKey() + "' because it has not finished connecting");
			} else {
				socketDest.writeJSON(channel, rawData);
			}
		}
	}

	private void sendPlayer(String sendingServer, String[] rcvStrings) {
		if (rcvStrings.length != 3) {
			mMain.getLogger().warning("Got sendPlayer command with invalid parameter count " + Integer.toString(rcvStrings.length) + "; expected 3");
			return;
		}

		// Message contains just player name, destination server, and player's UUID
		String destination = rcvStrings[0];
		String player = rcvStrings[1];
		UUID uuid = UUID.fromString(rcvStrings[2]);

		if (destination == null || player == null || uuid == null || destination.length() <= 0 || player.length() <= 0) {
			mMain.getLogger().warning("Got transfer message from '" + sendingServer + "' with invalid arguments");
			return;
		}

		// Get and validate the destination server
		ServerInfo serverInfo = mMain.getProxy().getServers().get(destination);
		if (serverInfo == null) {
			mMain.getLogger().warning("Cannot transfer player from '" + sendingServer + "' to unknown destination '" + destination + "'");
			return;
		}

		// Get and validate the player
		ProxiedPlayer playerInfo = mMain.getProxy().getPlayer(uuid);
		if (serverInfo == null) {
			mMain.getLogger().warning("Cannot transfer unknown player '" + player + "' from '" + sendingServer + "' to '" + destination + "'");
			return;
		}

		// Finally transfer the player
		playerInfo.connect(serverInfo, new Callback<Boolean>() {
			@Override
			public void done(Boolean arg0, Throwable arg1) {
				mMain.getLogger().info("Transferred '" + player + "' to '" + destination + "'");
			}
		});
	}
}
