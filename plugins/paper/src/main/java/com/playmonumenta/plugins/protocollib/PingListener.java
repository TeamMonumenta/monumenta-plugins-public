package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ConnectionSide;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.NetworkMarker;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketPostListener;
import com.playmonumenta.plugins.Plugin;
import java.util.HashMap;
import java.util.function.Consumer;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class PingListener extends PacketAdapter {
	private static final int BASE_ID = "the_portals rules!".hashCode();
	private static final HashMap<Integer, Triple<Player, Consumer<Integer>, Long>> mActionMap = new HashMap<>();
	private static int itemCounter = 0;

	public PingListener(Plugin plugin) {
		super(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.PONG);
	}

	public static class PingPostListener implements PacketPostListener {
		private final Runnable mRun;

		@Override
		public Plugin getPlugin() {
			return Plugin.getInstance();
		}

		@Override
		public void onPostEvent(PacketEvent event) {
			mRun.run();
		}

		public PingPostListener(Runnable runnable) {
			mRun = runnable;
		}
	}

	/**
	 * Sends a ping to a player, then performs an action using the player's ping. May be preferable to player.getPing(), as it doesn't use a weighted average.
	 * Please note the action is performed <u>asynchronously</u>.
	 *
	 * @param player         Player to check the ping of
	 * @param onPongReceived Consumer that consumes the ping when the player client responds
	 * @param timeout        Number of ticks to wait until timeout
	 * @param runOnTimeout   Whether the consumer should be run even if timeout occurs
	 * @param onTimeout      Called if timeout occurs
	 */
	public static void submitPingAction(Player player, Consumer<Integer> onPongReceived, int timeout, boolean runOnTimeout, @Nullable Runnable onTimeout) {
		int id = BASE_ID + itemCounter;
		itemCounter += 1;
		PacketContainer ping = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PING);
		ping.getIntegers().write(0, id);
		NetworkMarker marker = new NetworkMarker(ConnectionSide.SERVER_SIDE, PacketType.Play.Server.PING);
		marker.addPostListener(new PingPostListener(() -> {
			mActionMap.put(id, Triple.of(player, onPongReceived, System.currentTimeMillis()));
		}));
		ProtocolLibrary.getProtocolManager().sendServerPacket(player, ping, marker, true);
		// Check for timeout
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mActionMap.containsKey(id)) {
					mActionMap.remove(id);

					if (runOnTimeout) {
						onPongReceived.accept(timeout);
					}
					if (onTimeout != null) {
						onTimeout.run();
					}
				}
			}
		}.runTaskLater(Plugin.getInstance(), timeout);
	}

	@Override
	public void onPacketReceiving(PacketEvent event) {
		int id = event.getPacket().getIntegers().read(0);
		var entry = mActionMap.get(id);
		if (entry == null) {
			return;
		}
		Player submittedPlayer = entry.getLeft();
		if (!event.getPlayer().equals(submittedPlayer)) {
			// Chances of the id colliding with another plugin's ping/pong are slim...
			Bukkit.getLogger().warning("PingListener had a collision with another plugin's ping/pong response with id = " + id);
			// Chances of the id colliding while the *same* player is also queued is downright astronomical.
			return;
		}
		var action = entry.getMiddle();
		long timeSent = entry.getRight();
		action.accept((int) (System.currentTimeMillis() - timeSent));
		mActionMap.remove(id);
	}
}
