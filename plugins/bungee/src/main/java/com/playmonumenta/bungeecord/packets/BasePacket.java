package com.playmonumenta.bungeecord.packets;

import com.google.gson.JsonObject;
import com.playmonumenta.bungeecord.network.ClientSocket;
import com.playmonumenta.bungeecord.network.SocketManager;

public class BasePacket {
	public static final String PacketOperation = "Do.Not.Use.This.Packet";
	protected String mDestination;
	protected String mOperation;
	protected JsonObject mData;

	public BasePacket(String destination, String operation, JsonObject data) {
		mDestination = destination;
		mOperation = operation;
		mData = data;
	}

	public Boolean hasDestination() {
		return mDestination != null && !mDestination.isEmpty();
	}
	public boolean hasOperation() {
		return mOperation != null && !mOperation.isEmpty();
	}
	public boolean hasData() {
		return mData != null && mData.size() != 0;
	}

	public String getDestination() {
		return mDestination;
	}
	public String getOperation() {
		return mOperation;
	}
	public JsonObject getData() {
		return mData;
	}
	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		if (hasDestination()) {
			json.addProperty("dest", getDestination());
		}
		if (hasOperation()) {
			json.addProperty("op", getOperation());
		}
		if (hasData()) {
			json.add("data", getData());
		}
		return json;
	}

	public static void handlePacket(SocketManager manager, ClientSocket client, BasePacket packet) throws Exception {
		throw new Exception("BasePacket cannot be handled by bungee");
	}
}
