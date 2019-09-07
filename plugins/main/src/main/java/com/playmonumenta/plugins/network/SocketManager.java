package com.playmonumenta.plugins.network;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.packets.BasePacket;
import com.playmonumenta.plugins.packets.BroadcastCommandPacket;
import com.playmonumenta.plugins.packets.BungeeCommandPacket;
import com.playmonumenta.plugins.packets.BungeeErrorPacket;
import com.playmonumenta.plugins.packets.BungeeGetServerListPacket;
import com.playmonumenta.plugins.packets.ShardCommandPacket;
import com.playmonumenta.plugins.packets.ShardErrorPacket;
import com.playmonumenta.plugins.packets.BungeeHeartbeatPacket;
import com.playmonumenta.plugins.packets.BungeeHandshakePacket;
import com.playmonumenta.plugins.packets.BungeeSendPlayerPacket;
import com.playmonumenta.plugins.packets.ShardTransferPlayerDataPacket;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class SocketManager {
	private String mName = null;
	private Plugin mPlugin = null;
	private Socket mSocket = null;
	private Boolean mEnabled = false;
	private JsonReader mInput = null;
	private JsonWriter mOutput = null;
	private BukkitTask mHeartTask = null;
	private BukkitTask mAsyncTask = null;
	private BukkitRunnable mHeart = null;
	private BukkitRunnable mRunnable = null;

	public SocketManager(Plugin plugin, int port, String shardName) {
		mPlugin = plugin;
		mName = shardName;
		mRunnable = new BukkitRunnable(){
			@Override
			public void run() {
				_connect("bungee", port);
			}
		};
		mHeart = new BukkitRunnable(){
			@Override
			public void run() {
				sendPacket(new BungeeHeartbeatPacket());
			}
		};
	}

	public void close() {
		mEnabled = false;
		if (mAsyncTask != null) {
			mAsyncTask.cancel();
			mAsyncTask = null;
		}
		if (mHeartTask != null) {
			mHeartTask.cancel();
			mHeartTask = null;
		}
		if (mSocket != null &&
		    mSocket.isConnected() &&
		    !mSocket.isClosed()) {
			try {
				mSocket.close();
			} catch (Exception e) {
				//TODO: handle exception
				mPlugin.getLogger().warning("Error attempting to close socket");
				e.printStackTrace();
			}
			mSocket = null;
		}
		if (mInput != null) {
			try {
				mInput.close();
			} catch (Exception e) {
				//TODO: handle exception
				mPlugin.getLogger().warning("Error attempting to close socket input stream");
				e.printStackTrace();
			}
			mInput = null;
		}
		if (mOutput != null) {
			try {
				mOutput.close();
			} catch (Exception e) {
				//TODO: handle exception
				mPlugin.getLogger().warning("Error attempting to close socket output stream");
				e.printStackTrace();
			}
			mOutput = null;
		}
	}

	public void open() {
		close(); // If the socket is open, close it and stop the current loop.
		mEnabled = true; // Allows the new loop to run
		mAsyncTask = mRunnable.runTaskAsynchronously(mPlugin); // Start the loop
		mHeartTask = mHeart.runTaskTimerAsynchronously(mPlugin, 30 * 20, 30 * 20); // Send a heartbeat every 30 seconds
	}

	public Boolean sendPacket(String destination, String operation, JsonObject data) {
		if (mSocket != null && mSocket.isConnected() && !mSocket.isClosed()) {
			JsonObject raw = new JsonObject();
			if (destination != null) {
				raw.addProperty("dest", destination);
			}
			if (operation != null) {
				raw.addProperty("op", operation);
			}
			if (data != null) {
				raw.add("data", data);
			}
			try {
				Streams.write(raw, mOutput);
				mOutput.flush();
				return true;
			} catch (Exception e) {
				mPlugin.getLogger().warning("Error sending packet:");
				e.printStackTrace();
			}
		}
		return false;
	}

	public Boolean sendPacket(BasePacket packet) {
		if (mSocket != null && mSocket.isConnected() && !mSocket.isClosed()) {
			JsonObject raw = packet.toJson();
			try {
				Streams.write(raw, mOutput);
				mOutput.flush();
				return true;
			} catch (Exception e) {
				mPlugin.getLogger().warning("Error sending packet:");
				e.printStackTrace();
			}
		}
		return false;
	}

	// public Boolean send(BasePacket packet) {
	// 	return send(packet.getDestination(), packet.getOperation(), packet.getData());
	// }

	private void _connect(String address, int port) {
		int attempts = 0;
		while (mEnabled) {
			mPlugin.getLogger().info("Attempt "+attempts+" connecting to socket on "+address+":"+port);
			try {
				mSocket = new Socket(address, port);
				mInput = new JsonReader(new InputStreamReader(mSocket.getInputStream()));
				mOutput = new JsonWriter(new OutputStreamWriter(mSocket.getOutputStream()));
				mInput.setLenient(true); //  Both streams need to be lenient for the sockets to work.
				mOutput.setLenient(true); // Even if the JSON is perfect, the socket will fail if not lenient.
				mPlugin.getLogger().info("Connected to socket after "+attempts+" attempts");
				attempts = 0;
				sendPacket(new BungeeHandshakePacket(mName));
				while (!mSocket.isClosed()) {
					JsonElement raw = Streams.parse(mInput);
					if (raw != null && raw.isJsonObject()) {
						JsonObject rawData = raw.getAsJsonObject();
						String dest = null;
						String op = null;
						JsonObject data = null;
						if (rawData.has("dest") &&
						    rawData.get("dest").isJsonPrimitive() &&
						    rawData.get("dest").getAsJsonPrimitive().isString()) {
							dest = rawData.get("dest").getAsString();
						}
						if (rawData.has("op") &&
						    rawData.get("op").isJsonPrimitive() &&
						    rawData.get("op").getAsJsonPrimitive().isString()) {
							op = rawData.get("op").getAsString();
						}
						if (rawData.has("data") &&
						    rawData.get("data").isJsonObject()) {
							data = rawData.getAsJsonObject("data");
						}
						BasePacket compiled = new BasePacket(dest, op, data);
						if (op.equals(BasePacket.PacketOperation)) {
							BasePacket.handlePacket(mPlugin, compiled);
						} else if (op.equals(BroadcastCommandPacket.PacketOperation)) {
							BroadcastCommandPacket.handlePacket(mPlugin, compiled);
						} else if (op.equals(BungeeCommandPacket.PacketOperation)) {
							BungeeCommandPacket.handlePacket(mPlugin, compiled);
						} else if (op.equals(BungeeErrorPacket.PacketOperation)) {
							BungeeErrorPacket.handlePacket(mPlugin, compiled);
						} else if (op.equals(BungeeGetServerListPacket.PacketOperation)) {
							BungeeGetServerListPacket.handlePacket(mPlugin, compiled);
						} else if (op.equals(BungeeSendPlayerPacket.PacketOperation)) {
							BungeeSendPlayerPacket.handlePacket(mPlugin, compiled);
						} else if (op.equals(BungeeHeartbeatPacket.PacketOperation)) {
							BungeeHeartbeatPacket.handlePacket(mPlugin, compiled);
						} else if (op.equals(ShardCommandPacket.PacketOperation)) {
							ShardCommandPacket.handlePacket(mPlugin, compiled);
						} else if (op.equals(ShardErrorPacket.PacketOperation)) {
							ShardErrorPacket.handlePacket(mPlugin, compiled);
						} else if (op.equals(ShardTransferPlayerDataPacket.PacketOperation)) {
							ShardTransferPlayerDataPacket.handlePacket(mPlugin, compiled);
						} else {
							mPlugin.getLogger().warning(mName+" received unknown packet: "+op);
						}
					}
				}
			} catch (IOException e) {
				mPlugin.getLogger().warning("Socket Error: Connection");
				e.printStackTrace();
			} catch (JsonParseException e) {
				mPlugin.getLogger().warning("Socket Error: JSON Parse");
				e.printStackTrace();
			} catch (Exception e) {
				mPlugin.getLogger().warning("Socket Error: Misc");
				e.printStackTrace();
			}
			if (mEnabled) {
				// Something went wrong. Socket will retry, but let's wait.
				try {
					Thread.sleep(Math.min(attempts, 10) * 1000);
				} catch (Exception e2) {
					mPlugin.getLogger().warning("Error, can't sleep: ");
					e2.printStackTrace();
				}
				attempts++;
			}
		}
	}
}