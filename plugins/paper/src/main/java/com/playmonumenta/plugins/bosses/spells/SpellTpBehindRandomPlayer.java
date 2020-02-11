package com.playmonumenta.plugins.bosses.spells;

import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class SpellTpBehindRandomPlayer extends Spell {
	private static final int MAX_RANGE = 80;
	private static final int TP_DELAY = 50;

	private final Plugin mPlugin;
	private final Entity mLauncher;
	private final int mDuration;
	private final Random mRand = new Random();

	public SpellTpBehindRandomPlayer(Plugin plugin, Entity launcher, int duration) {
		mPlugin = plugin;
		mLauncher = launcher;
		mDuration = duration;
	}

	@Override
	public void run() {
		List<Player> players = PlayerUtils.playersInRange(mLauncher.getLocation(), 80);
		while (!players.isEmpty()) {
			Player target = players.get(mRand.nextInt(players.size()));

			/* Do not teleport to players in safezones */
			if (ZoneUtils.hasZoneProperty(target, ZoneProperty.RESIST_5)) {
				/* This player is in a safe area - don't tp to them */
				players.remove(target);
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

				// set newloc to target and slightly elevate it
				Location newloc = target.getLocation();
				newloc.setY(target.getLocation().getY() + 0.1f);
				World world = mLauncher.getWorld();
				// Move half a block at a time
				Vector vect = newloc.getDirection().multiply(-0.5f);
				// Back up vector
				Vector vectR = newloc.getDirection().multiply(0.2f);
				vect.setY(0);
				// don't teleport into/through a wall, teleport as close as possible instead
				boolean safe = true;
				boolean cancel = false;
				BoundingBox box = mLauncher.getBoundingBox();
				box.shift(newloc.clone().subtract(mLauncher.getLocation()));
				for (int i = 0; i < 6; i++) {
					newloc.add(vect);
					box.shift(vect);
					int j = 0;
					do {
						j++;
						safe = true;
						// Check if the entity overlaps with any of the surrounding blocks
						for (int x = -1; x <= 1 && safe; x++) {
							for (int y = -1; y <= 1 && safe; y++) {
								for (int z = -1; z <= 1 && safe; z++) {
									Block block = newloc.clone().add(x, y, z).getBlock();
									// If it overlaps with any, move it closer to target
									if (block.getBoundingBox().overlaps(box) && !block.isLiquid()) {
										newloc.add(vectR);
										box.shift(vectR);
										safe = false;
										cancel = true;
									}
								}
							}
						}
						// Keep looping until we're either safe, or we've backed up too far
					} while (!safe && j < 16);
					// If we backed up too far, tp to target directly.
					if (j > 15) {
						newloc = target.getLocation();
						cancel = true;
					} else if (cancel) {
						// If we had to back up, stop going forwards
						break;
					}
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
