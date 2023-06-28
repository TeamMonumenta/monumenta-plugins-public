package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPSpiral;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Whirlpool implements EliteFinisher {
	public static final String NAME = "Whirlpool";
	public static final int PORTAL_DURATION = 60;
	public static final int PORTAL_PERIOD = 30;

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		// "Borrowed" from com.playmonumenta.plugins.fishing.FishingCombatManager.drawWhirlpool
		PPSpiral spiral = new PPSpiral(Particle.WATER_WAKE, loc.clone().add(new Vector(0.0F, 0.16F, 0.0F)), 6).distanceFalloff(30);

		new BukkitRunnable() {
			final int mMaxRuns = PORTAL_DURATION / PORTAL_PERIOD;
			int mTimesRun = 0;

			@Override
			public void run() {
				spiral.spawnFull();
				loc.getWorld().playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.HOSTILE, 0.2f, 0.8f);
				loc.getWorld().playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.HOSTILE, 0.2f, 0.8f);

				if (mTimesRun >= mMaxRuns) {
					cancel();
					return;
				}
				mTimesRun++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, PORTAL_PERIOD);
	}

	@Override
	public Material getDisplayItem() {
		return Material.FISHING_ROD;
	}
}
