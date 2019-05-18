package com.playmonumenta.bungeecord.listeners;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.playmonumenta.bungeecord.Main;
import com.playmonumenta.bungeecord.utils.PacketUtils;

import de.myzelyam.api.vanish.BungeeVanishAPI;

import fr.rhaz.socket4mc.Bungee.BungeeSocketHandshakeEvent;
import fr.rhaz.socket4mc.Bungee.BungeeSocketJSONEvent;
import fr.rhaz.socketapi.SocketAPI.Server.SocketMessenger;

import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class EventListener implements Listener {
	final boolean mVanishEnabled;

	final Main mMain;

	// Collection of all connected sockets
	private static ConcurrentHashMap<String, SocketMessenger> mSockets;

	public EventListener(Main main) {
		mMain = main;
		mSockets = new ConcurrentHashMap<>();

		PluginManager mgr = mMain.getProxy().getPluginManager();

		if (mgr.getPlugin("PremiumVanish") != null) {
			mMain.getLogger().info("Vanish support enabled - PremiumVanish plugin detected");
			mVanishEnabled = true;
		} else {
			mMain.getLogger().info("Vanish support disabled - no plugin detected");
			mVanishEnabled = false;
		}
	}

	private void _joinLeaveEvent(ProxiedPlayer player, String operation, boolean isVanished) {
		if (!isVanished) {
			/* No vanish - send everyone the login message */

			BaseComponent[] msg = new ComponentBuilder(player.getName())
				.color(ChatColor.AQUA)
				.append(operation)
				.color(ChatColor.YELLOW)
				.create();

			for (ProxiedPlayer p : mMain.getProxy().getPlayers()) {
				p.sendMessage(msg);
			}

		} else {
			/*
			 * Vanish is enabled and player joined vanished
			 * Only send login message to other players that have perms to see it
			 */
			int useLevel = 0;
			for (int i = 5; i > 0; i--) {
				if (player.hasPermission("pv.use.level" + Integer.toString(i))) {
					useLevel = i;
					break;
				}
			}

			BaseComponent[] msg = new ComponentBuilder(player.getName())
				.color(ChatColor.AQUA)
				.append(operation)
				.color(ChatColor.YELLOW)
				.append(" vanished")
				.color(ChatColor.RED)
				.create();

			for (ProxiedPlayer p : mMain.getProxy().getPlayers()) {
				int seeLevel = -1;
				for (int i = 5; i > 0; i--) {
					if (p.hasPermission("pv.see.level" + Integer.toString(i))) {
						seeLevel = i;
						break;
					}
				}
				if (seeLevel >= useLevel) {
					p.sendMessage(msg);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
    public void postLoginEvent(PostLoginEvent event) {
		_joinLeaveEvent(event.getPlayer(), " joined the game",
		                mVanishEnabled && BungeeVanishAPI.isInvisible(event.getPlayer()));
    }

	@EventHandler(priority = EventPriority.LOW)
    public void playerDisconnectEvent(PlayerDisconnectEvent event) {
		_joinLeaveEvent(event.getPlayer(), " left the game",
		                mVanishEnabled && BungeeVanishAPI.isInvisible(event.getPlayer()));
    }

	// Every time a bungee server shard connects, add its socket to the list
	@EventHandler(priority = EventPriority.LOWEST)
    public void onHandshake(BungeeSocketHandshakeEvent e) {
		mMain.getLogger().info("'" + e.getName() + "' connected");
        mSockets.put(e.getName(), e.getMessenger());
    }

	@EventHandler(priority = EventPriority.LOWEST)
	public void onMessage(BungeeSocketJSONEvent e) {
		String channel = e.getChannel(); // The channel ("MyBukkitPlugin")
		String sendingServer = e.getName(); // The Spigot server name
		SocketMessenger sendingSocket = e.getMessenger();
		String rawData = e.getData(); // The data the plugin sent you

		// Re-register the sending server if it is not in the hashmap
		// This shouldn't be necessary, but Socket4MC seems to not call onHandshake
		// when servers connect before bungee is started
		if (sendingServer != null && sendingSocket != null && (!mSockets.containsKey(sendingServer))) {
			mMain.getLogger().info("Adding '" + sendingServer + "' to list of connected servers");
			mSockets.put(sendingServer, sendingSocket);
		}

		// Nothing to do with no payload
		if (rawData == null || rawData.length() <= 0) {
			mMain.getLogger().warning("Got message from '" + sendingServer + "' with empty data");
			return;
		}

		if ((!channel.startsWith("Monumenta.Bungee.Forward."))
			&& (!channel.startsWith("Monumenta.Bungee.Broadcast."))
			&& (!channel.equals("Monumenta.Bungee.SendPlayer"))
			&& (!channel.equals("Monumenta.Bungee.GetServerList"))
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
			if (bungeeForward(channel, sendingServer, rcvStrings, rawData) == false) {
				// If sending failed, send the message back with the first item as the previous channel
				String[] replyStrings = new String[rcvStrings.length + 1];
				replyStrings[0] = channel;
				System.arraycopy(rcvStrings, 0, replyStrings, 1, rcvStrings.length);

				try {
					sendingSocket.writeJSON("Monumenta.Bungee.Error.Forward", PacketUtils.encodeStrings(replyStrings));
				} catch (Exception ex) {
					mMain.getLogger().warning("Failed to send error packet response to '" + sendingServer + "'");
				}
			}
		} else if (channel.startsWith("Monumenta.Bungee.Broadcast.")) {
			bungeeBroadcast(channel, sendingServer, rawData);
		} else if (channel.equals("Monumenta.Bungee.SendPlayer")) {
			sendPlayer(sendingServer, rcvStrings);
		} else if (channel.equals("Monumenta.Bungee.GetServerList")) {
			GetServerList(sendingServer, rcvStrings);
		} else {
			mMain.getLogger().warning("Got message from '" + sendingServer + "' with unhandled channel '" + channel + "'");
		}
	}

	// Returns true if forwarding was successful, else returns false
	private boolean bungeeForward(String channel, String sendingServer, String[] rcvStrings, String rawData) {
		// First component of this type of packet is the destination server
		String destination = rcvStrings[0];

		if (destination == null || (!mSockets.containsKey(destination))) {
			mMain.getLogger().warning("Cannot forward message from '" + sendingServer + "' to unknown destination '" + destination + "'");
			mMain.getLogger().warning("mSockets list contents:");
			for (Map.Entry<String, SocketMessenger> entry : mSockets.entrySet()) {
				mMain.getLogger().warning("  " + entry.getKey());
			}
			return false;
		}

		// Look up the destination socket
		SocketMessenger socketDest = mSockets.get(destination);

		if ((!socketDest.isHandshaked()) || (!socketDest.isConnectedAndOpened())) {
			mMain.getLogger().warning("Cannot forward message from '" + sendingServer + "' because '" + destination + "' has not finished connecting");
			return false;
		}

		// Finally forward the message
		socketDest.writeJSON(channel, rawData);
		mMain.getLogger().info("Forwarded message from '" + sendingServer + "' to '" + destination + "'");

		return true;
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
		if (playerInfo == null) {
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

	private void GetServerList(String sendingServer, String[] rcvStrings) {
		if (rcvStrings.length != 2) {
			mMain.getLogger().warning("Got GetServerList command with invalid parameter count " + Integer.toString(rcvStrings.length) + "; expected 2");
			return;
		}

		// Message contains just player name and player's UUID
		String player = rcvStrings[0];
		UUID uuid = UUID.fromString(rcvStrings[1]);
		ProxiedPlayer playerInfo = mMain.getProxy().getPlayer(uuid);

		// Check arguments
		if (player == null || uuid == null || player.length() <= 0 || playerInfo == null) {
			mMain.getLogger().warning("Got GetServerList command from '" + sendingServer + "' with invalid arguments");
			return;
		}

		// Print list of servers to player
		TextComponent header = new TextComponent("Available Monumenta servers:");
		header.setColor(ChatColor.GOLD);
		playerInfo.sendMessage(header);
		for (Map.Entry<String, SocketMessenger> entry : mSockets.entrySet()) {
			SocketMessenger socketDest = entry.getValue();

			if (socketDest.isHandshaked() && socketDest.isConnectedAndOpened()) {
				TextComponent serverMessage = new TextComponent("  " + entry.getKey());
				serverMessage.setColor(ChatColor.GOLD);
				playerInfo.sendMessage(serverMessage);
			}
		}
	}
}
