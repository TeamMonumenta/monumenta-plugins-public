package com.playmonumenta.bossfights.spells;

import com.playmonumenta.bossfights.utils.Utils;

import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

public class SpellTpBehindRandomPlayer extends Spell {
	private Plugin mPlugin;
	private Entity mLauncher;
	private int mDuration;
	private Random mRand = new Random();

	public SpellTpBehindRandomPlayer(Plugin plugin, Entity launcher, int duration) {
		mPlugin = plugin;
		mLauncher = launcher;
		mDuration = duration;
	}

	@Override
	public void run() {
		List<Player> players = Utils.playersInRange(mLauncher.getLocation(), 80);
		Player target = players.get(mRand.nextInt(players.size()));
		launch(target);
		animation(target);
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
				Vector vect = newloc.getDirection().multiply(-3.0f);
				newloc.add(vect).setY(target.getLocation().getY() + 0.1f);
				mLauncher.teleport(newloc);
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
		Runnable teleport = new Runnable() {
			@Override
			public void run() {
				mLauncher.getWorld().playSound(mLauncher.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 3f, 0.7f);
			}
		};
		Runnable particle = new Runnable() {
			@Override
			public void run() {
				Location particleLoc = mLauncher.getLocation().add(new Location(mLauncher.getWorld(), -0.5f, 0f, 0.5f));
				particleLoc.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 10, 1, 1, 1, 0.03);
			}
		};
		scheduler.scheduleSyncDelayedTask(mPlugin, teleport, 49);
		for (int i = 0; i < 50; i++) {
			scheduler.scheduleSyncDelayedTask(mPlugin, particle, i);
		}
	}
}
