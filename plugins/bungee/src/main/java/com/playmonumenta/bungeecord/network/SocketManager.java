package com.playmonumenta.bungeecord.network;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import com.playmonumenta.bungeecord.Main;
import com.playmonumenta.bungeecord.packets.BasePacket;
import com.playmonumenta.bungeecord.packets.BroadcastCommandPacket;
import com.playmonumenta.bungeecord.packets.BungeeCommandPacket;
import com.playmonumenta.bungeecord.packets.BungeeErrorPacket;
import com.playmonumenta.bungeecord.packets.BungeeGetServerListPacket;
import com.playmonumenta.bungeecord.packets.BungeeGetVotesUnclaimedPacket;
import com.playmonumenta.bungeecord.packets.BungeeHandshakePacket;
import com.playmonumenta.bungeecord.packets.BungeeHeartbeatPacket;
import com.playmonumenta.bungeecord.packets.BungeeSendPlayerPacket;
import com.playmonumenta.bungeecord.packets.ShardCommandPacket;
import com.playmonumenta.bungeecord.packets.ShardTransferPlayerDataPacket;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class SocketManager {
	public final Main mMain;
	public final ProxyServer mProxy;
	private int mPort = 0;
	private ServerSocket mServer = null;
	private Map<String, ClientSocket> mClients = new HashMap<>();
	private Map<String, Queue<BasePacket>> mPacketQueue = new HashMap<>();
	private Runnable mRunnable = null;
	private ScheduledTask mTask = null;
	private Boolean mEnabled = false;

	public SocketManager(Main main, int port) {
		mProxy = ProxyServer.getInstance();
		mMain = main;
		mPort = port;
		mRunnable = new Runnable() {
			@Override
			public void run() {
				_listen();
			}
		};
	}

	public void start() {
		if (!mEnabled) {
			mEnabled = true;
			mTask = mProxy.getScheduler().runAsync(mMain, mRunnable);
		}
	}

	public void stop() {
		if (mEnabled) {
			mEnabled = false;
			mProxy.getScheduler().cancel(mTask);
		}
		for (Entry<String, ClientSocket> entry : mClients.entrySet()) {
			entry.getValue().close();
		}
	}

	public void clientHello(ClientSocket client, String name) {
		client.setName(name);
		if (mClients.containsKey(name)) {
			// Assume the shard is reconnecting and close the current connection
			mClients.get(name).close();
			mClients.remove(name);
		}
		mClients.put(name, client);
		// // TODO: Make the queue work when dynamic start/stop is implemented.
		// if (!mPacketQueue.containsKey(name)) {
		//  mPacketQueue.put(name, new LinkedList<>());
		// }
		// while (mPacketQueue.get(name).peek() != null) {
		//  client.sendPacket(mPacketQueue.get(name).element());
		// }
	}

	public void sortPacket(ClientSocket client, BasePacket packet) {
		mMain.getLogger().fine("Received packet from " + client.getName() + ": " + packet.getOperation());

		if (!packet.hasDestination()) {
			// Packet has no destination field. The destination is bungee itself.
			String op = packet.getOperation();
			try {
				if (op.equals(BasePacket.PacketOperation)) {
					BasePacket.handlePacket(this, client, packet);
				} else if (op.equals(BroadcastCommandPacket.PacketOperation)) {
					BroadcastCommandPacket.handlePacket(this, client, packet);
				} else if (op.equals(BungeeCommandPacket.PacketOperation)) {
					BungeeCommandPacket.handlePacket(this, client, packet);
				} else if (op.equals(BungeeErrorPacket.PacketOperation)) {
					BungeeErrorPacket.handlePacket(this, client, packet);
				} else if (op.equals(BungeeGetServerListPacket.PacketOperation)) {
					BungeeGetServerListPacket.handlePacket(this, client, packet);
				} else if (op.equals(BungeeHeartbeatPacket.PacketOperation)) {
					BungeeHeartbeatPacket.handlePacket(this, client, packet);
				} else if (op.equals(BungeeHandshakePacket.PacketOperation)) {
					BungeeHandshakePacket.handlePacket(this, client, packet);
				} else if (op.equals(BungeeSendPlayerPacket.PacketOperation)) {
					BungeeSendPlayerPacket.handlePacket(this, client, packet);
				} else if (op.equals(ShardCommandPacket.PacketOperation)) {
					ShardCommandPacket.handlePacket(this, client, packet);
				} else if (op.equals(ShardTransferPlayerDataPacket.PacketOperation)) {
					ShardTransferPlayerDataPacket.handlePacket(this, client, packet);
				} else if (op.equals(BungeeGetVotesUnclaimedPacket.PacketOperation)) {
					BungeeGetVotesUnclaimedPacket.handlePacket(this, client, packet);
				} else {
					mMain.getLogger().warning("Bungee received unknown packet: " + op);
				}
			} catch (Exception e) {
				mMain.getLogger().warning("Error processing packet");
				e.printStackTrace();
				client.sendPacket(new BungeeErrorPacket(e.getMessage(),
				                                        client.getName(),
				                                        packet.getDestination(),
				                                        packet.getOperation(),
				                                        packet.getData()));
			}
		} else if (packet.getDestination().equals("*")) {
			// Packet should be sent to all connected clients, including the sender.
			mMain.getLogger().fine("Received broadcast packet: " + packet.getOperation());
			for (Entry<String, ClientSocket> entry : mClients.entrySet()) {
				if (!entry.getValue().sendPacket(packet)) {
					// // TODO: Make the queue work when dynamic start/stop is implemented.
					// // Sending failed, queue packet and close socket
					// mPacketQueue.get(entry.getKey()).add(packet);
					// entry.getValue().close();
				}
			}
		} else {
			// Packet is addressed to a particular client
			//TODO: Poke offline servers to turn on to receive packets
			String dest = packet.getDestination();
			mMain.getLogger().fine("Received packet to " + dest + ": " + packet.getOperation());
			if (!mClients.containsKey(dest) ||
			    !mClients.get(dest).sendPacket(packet)) {
				// // TODO: Make the queue work when dynamic start/stop is implemented.
				// // Shard was never connected, or was disconnected. Queue the packet.
				// if (!mPacketQueue.containsKey(dest)) {
				//  mPacketQueue.put(dest, new LinkedList<>());
				// }
				// mPacketQueue.get(dest).add(packet);
				client.sendPacket(new BungeeErrorPacket("Shard not connected",
				                                        client.getName(),
				                                        packet.getDestination(),
				                                        packet.getOperation(),
				                                        packet.getData()));
			}
		}
	}

	public Collection<ClientSocket> getClients() {
		return mClients.values();
	}

	private void _listen() {
		int attempts = 0;
		while (mEnabled) {
			mMain.getLogger().info("Now listening for sockets on port " + mPort);
			try {
				mServer = new ServerSocket(mPort);
				while (mEnabled) {
					try {
						Socket socket = mServer.accept();
						ClientSocket client = new ClientSocket(mMain, this, socket);
						client.open();
					} catch (Exception e) {
						mMain.getLogger().warning("Error accepting socket: ");
						e.printStackTrace();
						break;
					}
				}
			} catch (Exception e) {
				mMain.getLogger().warning("Error creating socket server: ");
				e.printStackTrace();
			}
			// Socket server must close or restart.
			if (mEnabled) {
				// Server will restart, add a delay between attempts
				try {
					Thread.sleep(Math.min(attempts, 10) * 1000);
				} catch (Exception e) {
					mMain.getLogger().warning("Error, can't sleep: ");
					e.printStackTrace();
				}
				attempts++;
			}
		}
	}
}
