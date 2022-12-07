package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

public class SpellAxtalMeleeMinions extends Spell {
	private Plugin mPlugin;
	private Entity mLauncher;
	private int mCount;
	private int mScope;
	private int mRepeats;
	private int mNearbyRadius;
	private int mMaxNearby;

	public SpellAxtalMeleeMinions(Plugin plugin, Entity launcher, int count, int scope,
	                              int repeats, int nearbyRadius, int maxNearby) {
		mPlugin = plugin;
		mLauncher = launcher;
		mCount = count;
		mScope = scope;
		mRepeats = repeats;
		mNearbyRadius = nearbyRadius;
		mMaxNearby = maxNearby;
	}

	@Override
	public boolean canRun() {
		List<Entity> nearbyEntities = mLauncher.getNearbyEntities(mNearbyRadius, mNearbyRadius, mNearbyRadius);

		return nearbyEntities.stream().filter(e -> e.getType() == EntityType.SKELETON).count() < mMaxNearby;
	}

	@Override
	public void run() {
		animation();
		spawn();
	}

	@Override
	public int cooldownTicks() {
		return 160; // 8 seconds
	}

	public void spawn() {
		Location loc = mLauncher.getLocation();
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable singleSpawn = new Runnable() {
			@Override
			public void run() {
				int numberToSpawn = mCount + (FastUtils.RANDOM.nextInt(2 * mScope) - mScope);
				for (int j = 0; j < numberToSpawn; j++) {
					LibraryOfSoulsIntegration.summon(loc, "Soul");
				}
				for (Entity skelly : mLauncher.getNearbyEntities(0.2, 0.2, 0.2)) {
					if (skelly.getType() == EntityType.SKELETON) {
						double x = 0.5f * FastUtils.cos((double) FastUtils.RANDOM.nextInt(628) / 100);
						double z = 0.5f * FastUtils.sin((double) FastUtils.RANDOM.nextInt(628) / 100);
						skelly.setVelocity(new Vector(x, 0.5, z));
					}
				}
			}
		};
		for (int i = 0; i < mRepeats; i++) {
			scheduler.scheduleSyncDelayedTask(mPlugin, singleSpawn, 40 + 15 * i);
		}
	}

	public void animation() {
		Location loc = mLauncher.getLocation();
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable animLoop = new Runnable() {
			@Override
			public void run() {
				Location centerLoc = new Location(loc.getWorld(), loc.getX(), loc.getY() + 1, loc.getZ());
				mLauncher.teleport(new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ()));
				centerLoc.getWorld().playSound(centerLoc, Sound.BLOCK_PORTAL_AMBIENT, 1f, 0.5f);
				new PartialParticle(Particle.EXPLOSION_NORMAL, centerLoc, 20, 1, 1, 1, 0.01).spawnAsEntityActive(mLauncher);
			}
		};
		for (int i = 0; i < (40 + mRepeats * 15) / 3; i++) {
			scheduler.scheduleSyncDelayedTask(mPlugin, animLoop, i * 3);
		}
	}
}
