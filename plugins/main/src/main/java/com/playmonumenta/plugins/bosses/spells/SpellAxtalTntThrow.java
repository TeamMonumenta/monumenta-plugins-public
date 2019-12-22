package com.playmonumenta.plugins.bosses.spells;

import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.utils.Utils;

public class SpellAxtalTntThrow extends Spell {
	private Plugin mPlugin;
	private Entity mLauncher;
	private int mCount;
	private int mCooldown;
	Random mRand = new Random();

	public SpellAxtalTntThrow(Plugin plugin, Entity launcher, int count, int cooldown) {
		mPlugin = plugin;
		mLauncher = launcher;
		mCount = count;
		mCooldown = cooldown;
	}

	@Override
	public void run() {
		launch();
		animation();
	}

	@Override
	public int duration() {
		return 160; // 8 seconds
	}

	private void animation() {
		Location loc = mLauncher.getLocation();
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable particles1 = new Runnable() {
			@Override
			public void run() {
				mLauncher.teleport(loc);
				loc.getWorld().spawnParticle(Particle.LAVA, loc, 2, 0, 0, 0, 0.01);
			}
		};
		Runnable particles2 = new Runnable() {
			@Override
			public void run() {
				loc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 4, 0, 0, 0, 0.07);
				loc.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 1, 0.77F);
			}
		};
		loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_PIGMAN_ANGRY, 1, 0.77F);
		for (int i = 0; i < (40 + mCount * mCooldown); i++) {
			scheduler.scheduleSyncDelayedTask(mPlugin, particles1, i);
		}
		for (int i = 0; i < mCount; i++) {
			scheduler.scheduleSyncDelayedTask(mPlugin, particles2, 40 + i * mCooldown);
		}
	}

	private void launch() {
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable single_launch = new Runnable() {
			@Override
			public void run() {
				List<Player> plist = Utils.playersInRange(mLauncher.getLocation(), 100);
				if (plist.size() >= 1) {
					Player Target = plist.get(mRand.nextInt(plist.size()));
					Location sLoc = mLauncher.getLocation();
					try {
						Entity tnt = Utils.summonEntityAt(sLoc.add(0, 1.7, 0), EntityType.PRIMED_TNT, "{Fuse:50}");
						Location pLoc = Target.getLocation();
						Location tLoc = tnt.getLocation();
						Vector vect = new Vector(pLoc.getX() - tLoc.getX(), 0, pLoc.getZ() - tLoc.getZ());
						vect.normalize().multiply((pLoc.distance(tLoc)) / 20).setY(0.7f);
						tnt.setVelocity(vect);
					} catch (Exception e) {
						mPlugin.getLogger().warning("Summoned TNT but could not find TNT entity");
					}
				}
			}
		};
		for (int i = 0; i < mCount; i++) {
			scheduler.scheduleSyncDelayedTask(mPlugin, single_launch, 40 + i * mCooldown);
		}
	}
}

