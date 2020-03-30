package com.playmonumenta.plugins.network;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.packets.BasePacket;
import com.playmonumenta.plugins.packets.BroadcastCommandPacket;
import com.playmonumenta.plugins.packets.BungeeCheckRaffleEligibilityPacket;
import com.playmonumenta.plugins.packets.BungeeCommandPacket;
import com.playmonumenta.plugins.packets.BungeeGetServerListPacket;
import com.playmonumenta.plugins.packets.BungeeGetVotesUnclaimedPacket;
import com.playmonumenta.plugins.packets.BungeeSendPlayerPacket;
import com.playmonumenta.plugins.packets.ShardCommandPacket;
import com.playmonumenta.plugins.packets.ShardTransferPlayerDataPacket;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class SocketManager {
	private static final String CONSUMER_TAG = "consumerTag";
	private static final String BROADCAST_EXCHANGE_NAME = "broadcast";

	private static SocketManager INSTANCE = null;

	private final Gson mGson = new Gson();
	private final Plugin mPlugin;
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

	public SocketManager(Plugin plugin) throws Exception {
		mPlugin = plugin;

		String shardName = ServerProperties.getShardName();

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(ServerProperties.getRabbitHost());
		Connection connection = factory.newConnection();
		mChannel = connection.createChannel();

		/* Declare a broadcast exchange which routes messages to all attached queues */
		mChannel.exchangeDeclare(BROADCAST_EXCHANGE_NAME, "fanout");
		/* Declare the queue for this shard */
		mChannel.queueDeclare(shardName, false, false, false, null);
		/* Bind the queue to the exchange */
		mChannel.queueBind(shardName, BROADCAST_EXCHANGE_NAME, "");

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
				if (!obj.has("op") || !obj.get("op").isJsonPrimitive() || !obj.get("op").getAsJsonPrimitive().isString()) {
					throw new Exception("Rabbit message missing 'op': " + message);
				}
				if (!obj.has("source") || !obj.get("source").isJsonPrimitive() || !obj.get("source").getAsJsonPrimitive().isString()) {
					throw new Exception("Rabbit message missing 'source': " + message);
				}
				if (!obj.has("data") || !obj.get("data").isJsonObject()) {
					throw new Exception("Rabbit message missing 'data': " + message);
				}
			} catch (Exception ex) {
				mPlugin.getLogger().warning(ex.getMessage());
				/* Parsing this message failed - but ack it anyway, because it's not going to parse next time either */
				mChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
				return;
			}

			String op = obj.get("op").getAsString();
			String source = obj.get("source").getAsString();
			JsonObject data = obj.get("data").getAsJsonObject();

			/* Process the packet on the main thread */
			new BukkitRunnable() {
				@Override
				public void run() {
					try {
						doWork(op, source, data);
					} catch (Exception ex) {
						mPlugin.getLogger().warning("Failed to handle rabbit message '" + message + "'");
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
						mPlugin.getLogger().warning("Failed to acknowledge rabbit message '" + message + "'");
					}
				}
			}.runTask(plugin);
		};

		mConsumerAlive = true;
		mChannel.basicConsume(shardName, false, CONSUMER_TAG, deliverCallback,
		                      consumerTag -> {
			mConsumerAlive = false;
			if (mShutdown) {
				plugin.getLogger().info("RabbitMQ consumer has terminated");
			} else {
				plugin.getLogger().severe("RabbitMQ consumer has terminated unexpectedly - stopping the shard...");
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "stop");
			}
		});

		plugin.getLogger().info("Started RabbitMQ consumer");

		INSTANCE = this;
	}

	private void doWork(String op, String source, JsonObject data) throws Exception {
		mPlugin.getLogger().fine("Processing message from=" + source + " op=" + op);

		switch (op) {
			case BroadcastCommandPacket.PacketOperation:
				BroadcastCommandPacket.handlePacket(mPlugin, data);
				break;
			case BungeeCommandPacket.PacketOperation:
				BungeeCommandPacket.handlePacket(mPlugin, data);
				break;
			case BungeeGetServerListPacket.PacketOperation:
				BungeeGetServerListPacket.handlePacket(mPlugin, data);
				break;
			case BungeeSendPlayerPacket.PacketOperation:
				BungeeSendPlayerPacket.handlePacket(mPlugin, data);
				break;
			case ShardCommandPacket.PacketOperation:
				ShardCommandPacket.handlePacket(mPlugin, data);
				break;
			case ShardTransferPlayerDataPacket.PacketOperation:
				ShardTransferPlayerDataPacket.handlePacket(mPlugin, data);
				break;
			case BungeeGetVotesUnclaimedPacket.PacketOperation:
				BungeeGetVotesUnclaimedPacket.handlePacket(mPlugin, data);
				break;
			case BungeeCheckRaffleEligibilityPacket.PacketOperation:
				BungeeCheckRaffleEligibilityPacket.handlePacket(mPlugin, data);
				break;
			default:
				mPlugin.getLogger().warning("Received unknown rabbit op: " + op);
				break;
		}
	}

	public void stop() {
		mShutdown = true;
		if (mConsumerAlive) {
			try {
				mChannel.basicCancel(CONSUMER_TAG);
			} catch (Exception ex) {
				mPlugin.getLogger().info("Failed to cancel rabbit consumer");
			}
		}
	}

	public static boolean sendPacket(BasePacket packet) {
		if (INSTANCE == null) {
			return false;
		}
		return INSTANCE.sendPacketInternal(packet);
	}

	private boolean sendPacketInternal(BasePacket packet) {
		if (!packet.hasDestination()) {
			mPlugin.getLogger().warning("Can't send packet with no destination!");
			return false;
		} else if (!packet.hasOperation()) {
			mPlugin.getLogger().warning("Can't send packet with no operation!");
			return false;
		}

		/* Used in case the specific packet type overrides properties like expiration / time to live */
		AMQP.BasicProperties properties = packet.getProperties();

		JsonObject raw = packet.toJson();
		try {
			byte[] msg = raw.toString().getBytes(StandardCharsets.UTF_8);

			if (packet.getDestination().equals("*")) {
				/* Broadcast packet - send to the broadcast exchange to route to all queues */
				mChannel.basicPublish(BROADCAST_EXCHANGE_NAME, "", properties, msg);
			} else {
				/* Non-broadcast packet - send to the default exchange, routing to the appropriate queue */
				mChannel.basicPublish("", packet.getDestination(), properties, msg);
			}

			mPlugin.getLogger().fine("Sent message to=" + packet.getDestination() + " op=" + packet.getOperation());

			return true;
		} catch (Exception e) {
			mPlugin.getLogger().warning(String.format("Error sending packet to %s: %s", packet.getDestination(), packet.getOperation()));
			e.printStackTrace();
			return false;
		}
	}
}
