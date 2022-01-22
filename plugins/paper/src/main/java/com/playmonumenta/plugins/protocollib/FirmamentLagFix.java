package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.playmonumenta.plugins.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;

/**
 * Cancels the block update packets for neighboring blocks of a placed Firmament.
 * A cancelled block place event always sends these updates, but they cause issues with block placement client-side with high ping.
 */
public class FirmamentLagFix extends PacketAdapter {

	private static Block mFirmamentBlock;

	public FirmamentLagFix(Plugin plugin) {
		super(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.BLOCK_CHANGE);
	}

	// number of packets cancelled, should be 6 in total
	private static int mNumPackets = 0;

	public static void firmamentUsed(Block block) {
		mFirmamentBlock = block;
		mNumPackets = 0;
		// this is just a safety mechanism, the packet handling should clean up the block on its own
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> mFirmamentBlock = null);
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		if (mFirmamentBlock != null) {
			BlockPosition position = event.getPacket().getBlockPositionModifier().read(0);
			int dist = Math.abs(position.getX() - mFirmamentBlock.getX()) + Math.abs(position.getY() - mFirmamentBlock.getY()) + Math.abs(position.getZ() - mFirmamentBlock.getZ());
			if (dist != 1) { // not a neighbor for some reason, abort.
				mFirmamentBlock = null;
				return;
			}
			event.setCancelled(true);
			mNumPackets++;
			if (mNumPackets == 6) { // all 6 directions handled, we're done.
				mFirmamentBlock = null;
			}
		}
	}

}
