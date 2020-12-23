package com.playmonumenta.bungeecord.network;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.playmonumenta.bungeecord.Main;
import com.playmonumenta.bungeecord.packets.BasePacket;
import com.playmonumenta.bungeecord.packets.BungeeCheckRaffleEligibilityPacket;
import com.playmonumenta.bungeecord.packets.BungeeCommandPacket;
import com.playmonumenta.bungeecord.packets.BungeeGetVotesUnclaimedPacket;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import net.md_5.bungee.api.ProxyServer;

public class SocketManager {
	private static final String QUEUE_NAME = "bungee";
	private static final String CONSUMER_TAG = "consumerTag";

	private static SocketManager INSTANCE = null;

	public final Main mMain;
	public final ProxyServer mProxy;
	private final Gson mGson = new Gson();
	private final Channel mChannel;

	/*
	 * If mShutdown = false, this is expected to run normally
	 * If mShutdown = true, the server is already shutting down
	 */
	private boolean mShutdown = false;

	/*
	 * If mConsumerAlive = true, the consumer is running
	 * If mConsumerAlive = false, the consumer has terminated
	 */
	private boolean mConsumerAlive = false;

	public SocketManager(Main main, String rabbitHost) throws Exception {
		mProxy = ProxyServer.getInstance();
		mMain = main;

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(rabbitHost);
		Connection connection = factory.newConnection();
		mChannel = connection.createChannel();

		/* Declare the queue for this shard */
		mChannel.queueDeclare(QUEUE_NAME, false, false, false, null);

		/* Consumer to receive messages */
		DeliverCallback deliverCallback = (consumerTag, delivery) -> {
			final String message;
			final JsonObject obj;

			try {
				message = new String(delivery.getBody(), "UTF-8");

				obj = mGson.fromJson(message, JsonObject.class);
				if (obj == null) {
					throw new Exception("Failed to parse rabbit message as json: " + message);
				}
				if (!obj.has("channel") || !obj.get("channel").isJsonPrimitive() || !obj.get("channel").getAsJsonPrimitive().isString()) {
					throw new Exception("Rabbit message missing 'op': " + message);
				}
				if (!obj.has("source") || !obj.get("source").isJsonPrimitive() || !obj.get("source").getAsJsonPrimitive().isString()) {
					throw new Exception("Rabbit message missing 'source': " + message);
				}
				if (!obj.has("data") || !obj.get("data").isJsonObject()) {
					throw new Exception("Rabbit message missing 'data': " + message);
				}
			} catch (Exception ex) {
				mMain.getLogger().warning(ex.getMessage());
				/* Parsing this message failed - but ack it anyway, because it's not going to parse next time either */
				mChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
				return;
			}

			String op = obj.get("channel").getAsString();
			String source = obj.get("source").getAsString();
			JsonObject data = obj.get("data").getAsJsonObject();

			/* Process the packet. All bungee threads are async, so can do that in-line */
			try {
				doWork(op, source, data);
			} catch (Exception ex) {
				mMain.getLogger().warning("Failed to handle rabbit message '" + message + "'");
				ex.printStackTrace();
			}

			/*
			 * Always acknowledge messages after attempting to handle them, even if there's an error
			 * Don't want a failing message to get stuck in an infinite loop
			 */

			try {
				mChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
			} catch (IOException ex) {
				/*
				 * If the channel disconnects, we just won't ack this message
				 * It will be redelivered later
				 */
				mMain.getLogger().warning("Failed to acknowledge rabbit message '" + message + "'");
			}
		};

		mConsumerAlive = true;
		mChannel.basicConsume(QUEUE_NAME, false, CONSUMER_TAG, deliverCallback,
		                      consumerTag -> {
			mConsumerAlive = false;
			if (mShutdown) {
				main.getLogger().info("RabbitMQ consumer has terminated");
			} else {
				main.getLogger().severe("RabbitMQ consumer has terminated unexpectedly - stopping the shard...");
				main.getProxy().getPluginManager().dispatchCommand(main.getProxy().getConsole(), "end");
			}
		});

		main.getLogger().info("Started RabbitMQ consumer");

		INSTANCE = this;
	}

	private void doWork(String op, String source, JsonObject data) throws Exception {
		mMain.getLogger().fine("Processing message from=" + source + " op=" + op);

		switch (op) {
			case BungeeCommandPacket.PacketOperation:
				BungeeCommandPacket.handlePacket(mMain, source, data);
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
		mShutdown = true;
		if (mConsumerAlive) {
			try {
				mChannel.basicCancel(CONSUMER_TAG);
			} catch (Exception ex) {
				mMain.getLogger().info("Failed to cancel rabbit consumer");
			}
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

			mMain.getLogger().fine("Sent message to=" + packet.getDestination() + " op=" + packet.getOperation());

			return true;
		} catch (Exception e) {
			mMain.getLogger().warning(String.format("Error sending packet to %s: %s", packet.getDestination(), packet.getOperation()));
			e.printStackTrace();
			return false;
		}
	}
}
