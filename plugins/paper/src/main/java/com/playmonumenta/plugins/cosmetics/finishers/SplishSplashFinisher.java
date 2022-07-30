package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SplishSplashFinisher implements EliteFinisher {

	public static final String NAME = "Splish Splash";

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		World world = p.getWorld();
		world.playSound(loc, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, SoundCategory.PLAYERS, 1.f, 1.f);
		new PartialParticle(Particle.FALLING_WATER, loc.add(0, 1.5, 0), 64, .05, .5, .05).spawnAsPlayerActive(p);

		new BukkitRunnable() {
			double mRotation = 0;
			final Location mLoc = killedMob.getLocation();
			double mRadius = 0;

			@Override
			public void run() {
				mRadius += 0.25;
				for (int i = 0; i < 36; i += 1) {
					mRotation += 10;
					double radian1 = Math.toRadians(mRotation);
					mLoc.add(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
					//new particles
					new PartialParticle(Particle.WATER_BUBBLE, mLoc, 1, 0.15, 0.15, 0.15).spawnAsPlayerActive(p);
					new PartialParticle(Particle.WATER_WAKE, mLoc, 1, 0.15, 0.15, 0.15).spawnAsPlayerActive(p);
					mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);

				}
				if (mRadius >= 3) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 5, 1);

	}

	@Override
	public Material getDisplayItem() {
		return Material.WATER_BUCKET;
	}

}
