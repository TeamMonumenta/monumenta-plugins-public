package com.playmonumenta.bossfights.spells;

import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import com.playmonumenta.bossfights.utils.Utils;
import com.playmonumenta.plugins.safezone.SafeZoneManager.LocationType;

public class SpellTpBehindRandomPlayer extends Spell {
	private final com.playmonumenta.plugins.Plugin mMainPlugin;
	private final Plugin mPlugin;
	private final Entity mLauncher;
	private final int mDuration;
	private final Random mRand = new Random();

	public SpellTpBehindRandomPlayer(Plugin plugin, Entity launcher, int duration) {
		if (Bukkit.getPluginManager().isPluginEnabled("MonumentaMain")) {
			mMainPlugin = com.playmonumenta.plugins.Plugin.getInstance();
		} else {
			mMainPlugin = null;
		}

		mPlugin = plugin;
		mLauncher = launcher;
		mDuration = duration;
	}

	@Override
	public void run() {
		List<Player> players = Utils.playersInRange(mLauncher.getLocation(), 80);
		while (!players.isEmpty()) {
			Player target = players.get(mRand.nextInt(players.size()));

			if (mMainPlugin != null) {
				/* Do not teleport to players in safezones */
				LocationType zone = mMainPlugin.mSafeZoneManager.getLocationType(target);
				if (zone.equals(LocationType.Capital) || zone.equals(LocationType.SafeZone)) {
					/* This player is in a safe area - don't tp to them */
					players.remove(target);
				} else {
					launch(target);
					animation(target);
				}
			} else {
				launch(target);
				animation(target);
			}
		}
	}

	@Override
	public int duration() {
		return mDuration;
	}

	private void launch(Player target) {
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable teleport = new Runnable() {
			@Override
			public void run() {
				Location newloc = target.getLocation();
				World world = mLauncher.getWorld();
				Vector vect = newloc.getDirection().multiply(-3.0f);
				newloc.add(vect).setY(target.getLocation().getY() + 0.1f);
				world.spawnParticle(Particle.SPELL_WITCH, mLauncher.getLocation().add(0, mLauncher.getHeight() / 2, 0), 30, 0.25, 0.45, 0.25, 1);
				world.spawnParticle(Particle.SMOKE_LARGE, mLauncher.getLocation().add(0, mLauncher.getHeight() / 2, 0), 12, 0, 0.45, 0, 0.125);
				mLauncher.teleport(newloc);
				world.spawnParticle(Particle.SPELL_WITCH, newloc.clone().add(0, mLauncher.getHeight() / 2, 0), 30, 0.25, 0.45, 0.25, 1);
				world.spawnParticle(Particle.SMOKE_LARGE, newloc.clone().add(0, mLauncher.getHeight() / 2, 0), 12, 0, 0.45, 0, 0.125);
				mLauncher.getWorld().playSound(mLauncher.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 3f, 0.7f);
				if (mLauncher instanceof Mob) {
					((Mob)mLauncher).setTarget(target);
				}
			}
		};
		scheduler.scheduleSyncDelayedTask(mPlugin, teleport, 50);
	}

	private void animation(Player target) {
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 1.4f, 0.5f);

		Runnable particle = new Runnable() {
			@Override
			public void run() {
				Location particleLoc = mLauncher.getLocation().add(new Location(mLauncher.getWorld(), -0.5f, 0f, 0.5f));
				particleLoc.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 10, 1, 1, 1, 0.03);
			}
		};
		for (int i = 0; i < 50; i++) {
			scheduler.scheduleSyncDelayedTask(mPlugin, particle, i);
		}
	}
}
