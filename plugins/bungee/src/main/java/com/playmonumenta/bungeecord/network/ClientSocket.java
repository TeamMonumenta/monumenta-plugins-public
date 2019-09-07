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
import net.md_5.bungee.api.scheduler.ScheduledTask;

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
		mMain.getLogger().info("Sending packet to " + mName + ":");
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

	public Boolean close() {
		if (mEnabled) {
			try {
				mEnabled = false;
				mSocket.close();
				mInput.close();
				mOutput.close();
				return true;
			} catch (Exception e) {
				return false;
			}
		}
		return false;
	}

	public void open() {
		if (!mEnabled) {
			mEnabled = true;
			mProxy.getScheduler().runAsync(mMain, mRunnable);
		}
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
		while (mEnabled) {
			try {
				mInput = new JsonReader(new InputStreamReader(mSocket.getInputStream()));
				mOutput = new JsonWriter(new OutputStreamWriter(mSocket.getOutputStream()));
				mInput.setLenient(true);
				mOutput.setLenient(true);
				while (mEnabled) {
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
				close();
			} catch (IOException e) {
				mMain.getLogger().warning("Socket Error: Connection");
				e.printStackTrace();
				close();
			} catch (JsonParseException e) {
				mMain.getLogger().warning("Socket Error: JSON Parse");
				e.printStackTrace();
				close();
			} catch (Exception e) {
				mMain.getLogger().warning("Socket Error: Misc");
				e.printStackTrace();
				close();
			}
		}
	}
}
