package com.playmonumenta.plugins.bosses.spells;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellPushPlayersAway extends Spell {
	private Entity mLauncher;
	private int mRadius;
	private int mMaxNearTime;

	/* Tracks how long players have been too close */
	Map<UUID, Integer> mPlayerNearTimes = new HashMap<UUID, Integer>();

	/* Push players away that have been too close for too long */
	public SpellPushPlayersAway(Entity launcher, int radius, int maxNearTime) {
		mLauncher = launcher;
		mRadius = radius;
		mMaxNearTime = maxNearTime;
	}

	@Override
	public void run() {
		for (Player player : PlayerUtils.playersInRange(mLauncher.getLocation(), mRadius * 4)) {
			Integer nearTime = 0;
			Location pLoc = player.getLocation();
			if (pLoc.distance(mLauncher.getLocation()) < mRadius) {
				nearTime = mPlayerNearTimes.get(player.getUniqueId());
				if (nearTime == null) {
					nearTime = 0;
				}
				nearTime++;
				if (nearTime > mMaxNearTime) {
					Location lLoc = mLauncher.getLocation();
					Vector vect = new Vector(pLoc.getX() - lLoc.getX(), 0, pLoc.getZ() - lLoc.getZ());
					vect.normalize().setY(0.7f).multiply(2);
					player.setVelocity(vect);
				}
			}
			mPlayerNearTimes.put(player.getUniqueId(), nearTime);
		}
	}

	@Override
	public int duration() {
		return 1;
	}
}
