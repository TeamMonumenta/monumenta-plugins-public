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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.bossfights.utils.Utils;
import com.playmonumenta.plugins.safezone.SafeZoneManager.LocationType;

public class SpellTpBehindRandomPlayer extends Spell {
	private static final int MAX_RANGE = 80;
	private static final int TP_DELAY = 50;

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
					break;
				}
			} else {
				launch(target);
				animation(target);
				break;
			}
		}
	}

	@Override
	public int duration() {
		return mDuration;
	}

	private void launch(Player target) {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (target.getLocation().distance(mLauncher.getLocation()) > MAX_RANGE) {
					return;
				}

				Location newloc = target.getLocation();
				World world = mLauncher.getWorld();
				Vector vect = newloc.getDirection().multiply(-3.0f);
				newloc.add(vect).setY(target.getLocation().getY() + 0.1f);
				// dont teleport into a wall, teleport as close as possible instead
				while (newloc.getBlock().getType().isSolid()) {
					newloc.add(newloc.getDirection().multiply(0.2));
					// make sure its not partly clipped in the wall
					if (!newloc.getBlock().getType().isSolid())
						newloc.add(newloc.getDirection().multiply(0.35));
				}
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
		}.runTaskLater(mPlugin, TP_DELAY);
	}

	private void animation(Player target) {
		target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 1.4f, 0.5f);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				Location particleLoc = mLauncher.getLocation().add(new Location(mLauncher.getWorld(), -0.5f, 0f, 0.5f));
				particleLoc.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 10, 1, 1, 1, 0.03);

				if (mTicks > TP_DELAY) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}
}
