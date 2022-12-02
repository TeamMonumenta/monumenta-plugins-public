package com.playmonumenta.plugins.player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.adapters.VersionAdapter;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

/**
 * Tracks ender pearls thrown by players and handles teleportation to prevent various vanilla exploits that allow teleporting through solid walls and ceilings.
 */
public class EnderPearlTracker {

	private final Player mPlayer;
	private final EnderPearl mPearl;
	private final BukkitTask mTask;

	private final CircularFifoQueue<Location> mPastLocations = new CircularFifoQueue<>(100); // 5 seconds

	public EnderPearlTracker(Player player, EnderPearl pearl) {
		mPlayer = player;
		mPearl = pearl;
		mTask = Bukkit.getScheduler().runTaskTimer(Plugin.getInstance(), this::tick, 0, 1);
	}

	private void tick() {
		if (!mPearl.isValid()) {
			VersionAdapter versionAdapter = NmsUtils.getVersionAdapter();
			for (int i = mPastLocations.size() - 1; i >= 0; i--) {
				Location loc = mPastLocations.get(i);

				// require a clear 0.6 meter box around the pearl to even consider the location
				if (versionAdapter.hasCollision(loc.getWorld(), BoundingBox.of(loc, 0.3, 0.3, 0.3))) {
					continue;
				}

				// then check if there's space for a player bounding box nearby
				for (int y = -7; y <= 7; y++) {
					BoundingBox playerBox = BoundingBox.of(loc, 0.3, 0.9, 0.3);
					playerBox.shift(0, y * 0.1, 0);
					if (!versionAdapter.hasCollision(loc.getWorld(), playerBox)) {
						Location teleportLocation = new Location(loc.getWorld(), playerBox.getCenterX(), playerBox.getMinY(), playerBox.getCenterZ(),
							mPlayer.getLocation().getYaw(), mPlayer.getLocation().getPitch());
						if (!ZoneUtils.hasZoneProperty(teleportLocation, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
							// UNKNOWN cause to prevent the teleport runnable from changing the player's arrival location (and potentially allowing exploits again)
							mPlayer.teleport(teleportLocation, PlayerTeleportEvent.TeleportCause.UNKNOWN);
						}
						mTask.cancel();
						return;
					}
				}
			}
			mTask.cancel();
		} else {
			mPastLocations.add(mPearl.getLocation());
		}
	}

}
