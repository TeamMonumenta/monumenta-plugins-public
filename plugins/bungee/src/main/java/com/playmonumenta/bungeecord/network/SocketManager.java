package com.playmonumenta.bungeecord.network;

import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.playmonumenta.bungeecord.Main;
import com.playmonumenta.bungeecord.packets.BasePacket;
import com.playmonumenta.bungeecord.packets.BungeeCheckRaffleEligibilityPacket;
import com.playmonumenta.bungeecord.packets.BungeeCommandPacket;
import com.playmonumenta.bungeecord.packets.BungeeGetServerListPacket;
import com.playmonumenta.bungeecord.packets.BungeeGetVotesUnclaimedPacket;
import com.playmonumenta.bungeecord.packets.BungeeSendPlayerPacket;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class SocketManager {
	private static final String QUEUE_NAME = "bungee";
	private static final String CONSUMER_TAG = "consumerTag";

	private static SocketManager INSTANCE = null;

	public final Main mMain;
	public final ProxyServer mProxy;
	private final Gson mGson = new Gson();
	private final Runnable mRunnable;
	private final ScheduledTask mTask;
	private final Channel mChannel;

	public SocketManager(Main main, String rabbitHost) throws Exception {
		mProxy = ProxyServer.getInstance();
		mMain = main;

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(rabbitHost);
		Connection connection = factory.newConnection();
		mChannel = connection.createChannel();

		/* Consumer to receive messages */
		mChannel.queueDeclare(QUEUE_NAME, false, false, false, null);

		DeliverCallback deliverCallback = (consumerTag, delivery) -> {
			String message = new String(delivery.getBody(), "UTF-8");

			try {
				doWork(message);
			} catch (Exception ex) {
				mMain.getLogger().warning("Failed to handle rabbit message '" + message + "'");
				ex.printStackTrace();
			} finally {
				mChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
			}
		};

		mRunnable = new Runnable() {
			@Override
			public void run() {
				try {
					mChannel.basicConsume(QUEUE_NAME, false, CONSUMER_TAG, deliverCallback, shutdownCallback -> { /* TODO */ });
				} catch (Exception ex) {
					/* TODO: Better error? This will kill the shard... */
					ex.printStackTrace();
				}
			}
		};
		mTask = mProxy.getScheduler().runAsync(mMain, mRunnable);

		INSTANCE = this;
	}

	private void doWork(String task) throws Exception {
		JsonObject obj = null;
		try {
			obj = mGson.fromJson(task, JsonObject.class);
		} catch (JsonParseException e) {
			obj = null;
		}
		if (obj == null) {
			mMain.getLogger().warning("Failed to parse rabbit message as json: " + task);
			return;
		}

		if (!obj.has("op") || !obj.get("op").isJsonPrimitive() || !obj.get("op").getAsJsonPrimitive().isString()) {
			mMain.getLogger().warning("Rabbit message missing 'op': " + task);
			return;
		}
		if (!obj.has("source") || !obj.get("source").isJsonPrimitive() || !obj.get("source").getAsJsonPrimitive().isString()) {
			mMain.getLogger().warning("Rabbit message missing 'source': " + task);
			return;
		}
		if (!obj.has("data") || !obj.get("data").isJsonObject()) {
			mMain.getLogger().warning("Rabbit message missing 'data': " + task);
			return;
		}
		String op = obj.get("op").getAsString();
		String source = obj.get("source").getAsString();
		JsonObject data = obj.get("data").getAsJsonObject();

		/* TODO: Make this debug later */
		mMain.getLogger().info("Processing message from=" + source + " op=" + op);

		switch (op) {
			case BungeeCommandPacket.PacketOperation:
				BungeeCommandPacket.handlePacket(mMain, source, data);
				break;
			case BungeeGetServerListPacket.PacketOperation:
				BungeeGetServerListPacket.handlePacket(mMain, source, data);
				break;
			case BungeeSendPlayerPacket.PacketOperation:
				BungeeSendPlayerPacket.handlePacket(mMain, source, data);
				break;
			case BungeeGetVotesUnclaimedPacket.PacketOperation:
				BungeeGetVotesUnclaimedPacket.handlePacket(mMain, source, data);
				break;
			case BungeeCheckRaffleEligibilityPacket.PacketOperation:
				BungeeCheckRaffleEligibilityPacket.handlePacket(mMain, source, data);
				break;
			default:
				mMain.getLogger().warning("Received unknown rabbit op: " + op);
				break;
		}
	}

	public void stop() {
		try {
			mChannel.basicCancel(CONSUMER_TAG);
		} catch (Exception ex) {
			mMain.getLogger().info("Failed to cancel rabbit consumer");
		}
		try {
			mTask.cancel();
		} catch (Exception ex) {
			mMain.getLogger().info("Failed to cancel async rabbit consumer task");
		}
	}

	public static boolean sendPacket(BasePacket packet) {
		if (INSTANCE != null) {
			return INSTANCE.sendPacketInternal(packet);
		}
		return false;
	}

	private boolean sendPacketInternal(BasePacket packet) {
		if (!packet.hasDestination()) {
			mMain.getLogger().warning("Can't send packet with no destination!");
			return false;
		} else if (!packet.hasOperation()) {
			mMain.getLogger().warning("Can't send packet with no operation!");
			return false;
		}

		JsonObject raw = packet.toJson();
		try {
			mChannel.basicPublish("", packet.getDestination(), null, raw.toString().getBytes(StandardCharsets.UTF_8));

			/* TODO: Make this debug later */
			mMain.getLogger().info("Sent message to=" + packet.getDestination() + " op=" + packet.getOperation());

			return true;
		} catch (Exception e) {
			mMain.getLogger().warning(String.format("Error sending packet to %s: %s", packet.getDestination(), packet.getOperation()));
			e.printStackTrace();
			return false;
		}
	}
}
