package com.playmonumenta.bossfights.spells;

import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

public class SpellAxtalMeleeMinions implements Spell {
	private Plugin mPlugin;
	private Entity mLauncher;
	private int mCount;
	private int mScope;
	private int mRepeats;
	private int mNearbyRadius;
	private int mMaxNearby;
	private Random mRand = new Random();

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
	public int duration() {
		return 160; // 8 seconds
	}

	public void spawn() {
		Location loc = mLauncher.getLocation();
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable single_spawn = new Runnable() {
			@Override
			public void run() {
				int nb_to_spawn = mCount + (mRand.nextInt(2 * mScope) - mScope);
				for (int j = 0; j < nb_to_spawn; j++) {
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "summon skeleton " + loc.getX() + " " + loc.getY() + " " + loc.getZ() + " {CustomName:\"\\\"Soul\\\"\",CustomNameVisible:1,Tags:[\"Soul\"],ArmorItems:[{},{},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{display:{color:12430010}}},{id:\"minecraft:skeleton_skull\",Count:1b}],Attributes:[{Name:generic.maxHealth,Base:11},{Name:generic.attackDamage,Base:6}],Health:11.0f,DeathLootTable:\"minecraft:empty\",Team:\"Tlax\",ActiveEffects:[{Id:14b,Amplifier:1b,Duration:999999},{Id:20b,Amplifier:0b,Duration:999999}],Silent:1b}");
				}
				for (Entity skelly : mLauncher.getNearbyEntities(0.2, 0.2, 0.2)) {
					if (skelly.getType() == EntityType.SKELETON) {
						double x = 0.5f * Math.cos((double)mRand.nextInt(628) / 100);
						double z = 0.5f * Math.sin((double)mRand.nextInt(628) / 100);
						skelly.setVelocity(new Vector(x, 0.5, z));
					}
				}
			}
		};
		for (int i = 0; i < mRepeats; i++) {
			scheduler.scheduleSyncDelayedTask(mPlugin, single_spawn, 40 + 15 * i);
		}
	}

	public void animation() {
		Location loc = mLauncher.getLocation();
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable anim_loop = new Runnable() {
			@Override
			public void run() {
				Location centerLoc = new Location(loc.getWorld(), loc.getX(), loc.getY() + 1, loc.getZ());
				mLauncher.teleport(new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ()));
				centerLoc.getWorld().playSound(centerLoc, Sound.BLOCK_PORTAL_AMBIENT, 1f, 0.5f);
				centerLoc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, centerLoc, 20, 1, 1, 1, 0.01);
			}
		};
		for (int i = 0; i < (40 + mRepeats * 15) / 3; i++) {
			scheduler.scheduleSyncDelayedTask(mPlugin, anim_loop, i * 3);
		}
	}
}
