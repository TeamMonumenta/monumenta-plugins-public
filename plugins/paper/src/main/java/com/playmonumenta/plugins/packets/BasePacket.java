package com.playmonumenta.plugins.packets;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.rabbitmq.client.AMQP;

public abstract class BasePacket {
	private final String mSource;
	private final String mDestination;
	private final String mOperation;
	private final JsonObject mData;

	public BasePacket(String destination, String operation) {
		mSource = ServerProperties.getShardName();
		mDestination = destination;
		mOperation = operation;
		mData = new JsonObject();
	}

	public BasePacket() throws Exception {
		throw new Exception("This shard can't generate this packet");
	}

	public boolean hasSource() {
		return mSource != null && !mSource.isEmpty();
	}

	public boolean hasDestination() {
		return mDestination != null && !mDestination.isEmpty();
	}

	public boolean hasOperation() {
		return mOperation != null && !mOperation.isEmpty();
	}

	public boolean hasData() {
		return mData != null && mData.size() != 0;
	}

	public String getSource() {
		return mSource;
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

	public AMQP.BasicProperties getProperties() {
		return null;
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		if (hasSource()) {
			json.addProperty("source", mSource);
		}
		if (hasDestination()) {
			json.addProperty("dest", mDestination);
		}
		if (hasOperation()) {
			json.addProperty("op", mOperation);
		}
		if (hasData()) {
			json.add("data", mData);
		}
		return json;
	}
}
