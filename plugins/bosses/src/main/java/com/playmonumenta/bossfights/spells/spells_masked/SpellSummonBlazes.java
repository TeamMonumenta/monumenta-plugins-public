package com.playmonumenta.bossfights.spells.spells_masked;

import java.util.EnumSet;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import com.playmonumenta.bossfights.bosses.Masked;
import com.playmonumenta.bossfights.spells.Spell;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellSummonBlazes extends Spell {

	private static final int MAX_NEARBY_BLAZES_MULTIPLIER = 4;
	private static final int MAX_BLAZES_PER_SPAWN = 4;
	private static final int SPAWN_CYCLES = 2;
	private static final int DURATION = 20 * 8;
	private Plugin mPlugin;
	private Entity mLauncher;
	private Random mRand = new Random();

	public SpellSummonBlazes(Plugin plugin, Entity launcher) {
		mPlugin = plugin;
		mLauncher = launcher;
	}

	// Only run if there are fewer blazes than the multiplier * # of players.
	@Override
	public boolean canRun() {
		return EntityUtils.getNearbyMobs(mLauncher.getLocation(), Masked.DETECTION_RANGE, EnumSet.of(EntityType.BLAZE)).size()
		       < PlayerUtils.getNearbyPlayers(mLauncher.getLocation(), Masked.DETECTION_RANGE).size() * MAX_NEARBY_BLAZES_MULTIPLIER;
	}

	@Override
	public void run() {
		Location loc = mLauncher.getLocation();
		int count = Math.min(MAX_BLAZES_PER_SPAWN, PlayerUtils.getNearbyPlayers(loc, Masked.DETECTION_RANGE).size());
		animation(loc, SPAWN_CYCLES);
		spawn(loc, count, SPAWN_CYCLES);
	}

	@Override
	public int duration() {
		return DURATION;
	}

	private void spawn(Location loc, int count, int repeats) {
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable single_spawn = new Runnable() {
			@Override
			public void run() {
				for (int j = 0; j < count; j++) {
					Entity blaz = loc.getWorld().spawnEntity(loc, EntityType.BLAZE);
					double x = 0.5f * Math.cos((double)mRand.nextInt(628) / 100);
					double z = 0.5f * Math.sin((double)mRand.nextInt(628) / 100);
					blaz.setVelocity(new Vector(x, 0.3, z));
				}
			}
		};
		for (int i = 0; i < repeats; i++) {
			scheduler.scheduleSyncDelayedTask(mPlugin, single_spawn, 45 + 5 * i);
		}
	}

	private void animation(Location loc, int repeats) {
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable anim_loop = new Runnable() {
			@Override
			public void run() {
				Location centerLoc = new Location(loc.getWorld(), loc.getX(), loc.getY() + 1, loc.getZ());
				Location particleLoc = new Location(loc.getWorld(), 0, 0, 0);
				mLauncher.teleport(loc);
				centerLoc.getWorld().playSound(centerLoc, Sound.BLOCK_PORTAL_AMBIENT, 1f, 2f);
				for (int j = 0; j < 5; j++) {
					while (particleLoc.distance(centerLoc) > 2) {
						particleLoc.setX(loc.getX() + ((double)(mRand.nextInt(4000) - 2000) / 1000));
						particleLoc.setZ(loc.getZ() + ((double)(mRand.nextInt(4000) - 2000) / 1000));
						particleLoc.setY(loc.getY() + ((double)(mRand.nextInt(4000) - 2000) / 1000));
					}
					particleLoc.getWorld().spawnParticle(Particle.LAVA, particleLoc, 4, 0, 0, 0, 0.01);
					particleLoc.setX(0);
					particleLoc.setY(0);
					particleLoc.setZ(0);
				}
			}
		};
		for (int i = 0; i < (45 + 5 * repeats) / 3; i++) {
			scheduler.scheduleSyncDelayedTask(mPlugin, anim_loop, i * 3);
		}
	}
}
