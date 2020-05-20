package com.playmonumenta.plugins.bosses.spells;

import java.util.List;

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

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellAxtalTntThrow extends Spell {
	private Plugin mPlugin;
	private Entity mLauncher;
	private int mCount;
	private int mCooldown;

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
		Runnable singleLaunch = new Runnable() {
			@Override
			public void run() {
				List<Player> plist = PlayerUtils.playersInRange(mLauncher.getLocation(), 100);
				if (plist.size() >= 1) {
					Player target = plist.get(FastUtils.RANDOM.nextInt(plist.size()));
					Location sLoc = mLauncher.getLocation();
					try {
						Entity tnt = EntityUtils.getSummonEntityAt(sLoc.add(0, 1.7, 0), EntityType.PRIMED_TNT, "{Fuse:50}");
						Location pLoc = target.getLocation();
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
			scheduler.scheduleSyncDelayedTask(mPlugin, singleLaunch, 40 + i * mCooldown);
		}
	}
}

