package com.playmonumenta.bungeecord.network;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.playmonumenta.bungeecord.Main;
import com.playmonumenta.bungeecord.packets.BasePacket;

import net.md_5.bungee.api.ProxyServer;

public class ClientSocket {
	public final Main mMain;
	public final SocketManager mManager;
	public final ProxyServer mProxy;
	private Socket mSocket;
	private Runnable mRunnable;
	private JsonReader mInput;
	private JsonWriter mOutput;
	private String mName;
	private Boolean mEnabled = false;

	public enum Status {
		NEW,
		OPEN,
		CLOSED
	}

	public ClientSocket(Main main, SocketManager manager, Socket socket) {
		mMain = main;
		mManager = manager;
		mProxy = ProxyServer.getInstance();
		mSocket = socket;
		mName = null;
		mRunnable = new Runnable() {
			@Override
			public void run() {
				_listen();
			}
		};
	}

	public Boolean sendPacket(BasePacket packet) {
		mMain.getLogger().fine("Sending packet to " + mName + ":");
		try {
			Streams.write(packet.toJson(), mOutput);
			mOutput.flush();
			return true;
		} catch (Exception e) {
			//TODO: handle exception
			mMain.getLogger().warning("Error sending packet to " + mName + ":");
			e.printStackTrace();
			return false;
		}
	}

	public void close() {
		mEnabled = false;
		if (mSocket != null) {
			try {
				mSocket.close();
			} catch (Exception e) {
				//TODO: handle exception
				mMain.getLogger().warning("Error attempting to close socket");
				e.printStackTrace();
			}
			mSocket = null;
		}
		if (mInput != null) {
			try {
				mInput.close();
			} catch (Exception e) {
				//TODO: handle exception
				mMain.getLogger().warning("Error attempting to close socket input stream");
				e.printStackTrace();
			}
			mInput = null;
		}
		if (mOutput != null) {
			try {
				mOutput.close();
			} catch (Exception e) {
				//TODO: handle exception
				mMain.getLogger().warning("Error attempting to close socket output stream");
				e.printStackTrace();
			}
			mOutput = null;
		}
	}

	public void open() {
		mEnabled = true;
		mProxy.getScheduler().runAsync(mMain, mRunnable);
	}

	public Status getStatus() {
		if (!mSocket.isConnected()) {
			return Status.NEW;
		}
		if (!mSocket.isClosed()) {
			return Status.OPEN;
		}
		return Status.CLOSED;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		mName = name;
	}

	private void _listen() {
		try {
			// Create a reader/writer for this socket
			mInput = new JsonReader(new InputStreamReader(mSocket.getInputStream()));
			mOutput = new JsonWriter(new OutputStreamWriter(mSocket.getOutputStream()));
			// Both streams *must* be set as lenient to accommodate for how sockets communicate
			mInput.setLenient(true);
			mOutput.setLenient(true);
			while (mEnabled) {
				// Use gson's Streams module to read one full object.
				JsonElement in = Streams.parse(mInput);
				if (in.isJsonObject()) {
					JsonObject raw = in.getAsJsonObject();
					String dest = null;
					String op = null;
					JsonObject data = null;
					if (raw.has("dest") &&
						raw.get("dest").isJsonPrimitive() &&
						raw.getAsJsonPrimitive("dest").isString()) {
						dest = raw.get("dest").getAsString();
					}
					if (raw.has("op") &&
						raw.get("op").isJsonPrimitive() &&
						raw.getAsJsonPrimitive("op").isString()) {
						op = raw.get("op").getAsString();
					}
					if (raw.has("data") &&
						raw.get("data").isJsonObject()) {
						data = raw.getAsJsonObject("data");
					}
					mManager.sortPacket(this, new BasePacket(dest, op, data));
				}
			}
		} catch (SocketException e) {
			mMain.getLogger().warning("Socket Error: Closed");
			e.printStackTrace();
		} catch (IOException e) {
			mMain.getLogger().warning("Socket Error: Connection");
			e.printStackTrace();
		} catch (JsonParseException e) {
			mMain.getLogger().warning("Socket Error: JSON Parse");
			e.printStackTrace();
		} catch (Exception e) {
			mMain.getLogger().warning("Socket Error: Misc");
			e.printStackTrace();
		}
		// If execution reaches this point, it means either the socket was closed or threw an error
		// In either case, close the socket and stop the listener. The client can reconnect.
		close();
	}
}
