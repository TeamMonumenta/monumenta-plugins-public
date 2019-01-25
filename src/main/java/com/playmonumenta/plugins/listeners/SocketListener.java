package com.playmonumenta.plugins.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.packets.HeartbeatPacket;
import com.playmonumenta.plugins.utils.PacketUtils;

import fr.rhaz.socket4mc.Bukkit.BukkitSocketHandshakeEvent;
import fr.rhaz.socket4mc.Bukkit.BukkitSocketJSONEvent;

public class SocketListener implements Listener {
	Plugin mPlugin = null;

	public SocketListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onConnect(BukkitSocketHandshakeEvent e) {
		mPlugin.mSocketClient = e.getClient();

		/* This deferred task helps work around a bug in Socket4MC/SocketAPI
		 * If bungee restarts when the servers are already up, the sockets connect
		 * before Monumenta is loaded, so the onHandshake() call doesn't fire, which means
		 * the Monumenta Bungee plugin doesn't know these servers are connected.
		 * They connect just fine though.
		 *
		 * So this deferred task pokes bungee a bit later to remind it this server is connected
		 */
		new BukkitRunnable() {
			Integer heartbeat = 0;
			public void run() {
					try {
						mPlugin.getLogger().info("Sending heartbeat message to bungee");
						// Send a simple hello message to bungee
						PacketUtils.SendPacket(mPlugin, new HeartbeatPacket());
					} catch (Exception ex) {
						mPlugin.getLogger().severe("Caught exception sending HeartbeatPacket: " + ex);
						ex.printStackTrace();
					}

				if (++heartbeat == 5) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 200, 1000);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onMessage(BukkitSocketJSONEvent e) {
		String channel = e.getChannel();
		String data = e.getData();

		try {
			PacketUtils.ProcessPacket(mPlugin, channel, data);
		} catch (Exception ex) {
			mPlugin.getLogger().severe("Caught exception handling packet on channel '" + channel + "'");
			ex.printStackTrace();
			return;
		}
	}
}
