package com.playmonumenta.bossfights.spells;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

public class SpellDetectionCircle implements Spell {
	private Plugin mPlugin;
	private int mRadius;
	private int mDuration;
	private Location mCenter;
	private Location mTarget;
	private Random mRand = new Random();
	private int runs_left;
	private int taskID;

	public SpellDetectionCircle(Plugin plugin, Location center, int radius, int duration, Location target) {
		mPlugin = plugin;
		mRadius = radius;
		mDuration = duration;
		mCenter = center;
		mTarget = target;
	}

	@Override
	public void run() {
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		runs_left = mDuration;
		Runnable loop = new Runnable() {
			@Override
			public void run() {
				int  n = mRand.nextInt(50) + 100;
				double precision = n;
				double increment = (2 * Math.PI) / precision;
				Location particleLoc = new Location(mCenter.getWorld(), 0, mCenter.getY() + 5, 0);
				double rad = mRadius;
				double angle = 0;
				for (int j = 0; j < precision; j++) {
					angle = (double)j * increment;
					particleLoc.setX(mCenter.getX() + (rad * Math.cos(angle)));
					particleLoc.setZ(mCenter.getZ() + (rad * Math.sin(angle)));
					particleLoc.setY(mCenter.getY() + 5 * (double)(mRand.nextInt(120) - 60) / (60));
					particleLoc.getWorld().spawnParticle(Particle.SMOKE_LARGE, particleLoc, 1, 0.02, 0.02, 0.02, 0);
				}

				for (Player player : Bukkit.getServer().getOnlinePlayers()) {
					if (player.getLocation().distance(mCenter) < mRadius && player.getGameMode() == GameMode.SURVIVAL) {
						mTarget.getBlock().setType(Material.REDSTONE_BLOCK);
						scheduler.cancelTask(taskID);
						break;
					}
				}
				if (runs_left <= 0) {
					scheduler.cancelTask(taskID);
				}
				runs_left -= 5;
			}
		};
		taskID = scheduler.scheduleSyncRepeatingTask(mPlugin, loop, 1L, 5L);
	}

	@Override
	public int duration() {
		return 1;
	}
}
