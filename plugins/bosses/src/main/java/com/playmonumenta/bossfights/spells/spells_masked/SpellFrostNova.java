package com.playmonumenta.bossfights.spells.spells_masked;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.bossfights.spells.Spell;
import com.playmonumenta.bossfights.utils.Utils;

public class SpellFrostNova extends Spell {

	private Plugin mPlugin;
	private Random mRand = new Random();
	private Entity mLauncher;
	private int mRadius;
	private int mTime;

	public SpellFrostNova(Plugin plugin, Entity launcher, int radius, int time) {
		mPlugin = plugin;
		mLauncher = launcher;
		mRadius = radius;
		mTime = time;
	}

	@Override
	public void run() {
		BukkitRunnable loop = new BukkitRunnable() {
			final Location loc = mLauncher.getLocation().clone();
			int w = 0;

			@Override
			public void run() {
				Location centerLoc = loc.clone().add(0, 1, 0);
				if (w < mTime) {
					mLauncher.teleport(loc);
					centerLoc.getWorld().playSound(centerLoc, Sound.BLOCK_SNOW_STEP, (float)mRadius / 7, (float)(0.5 + mRand.nextInt(150) / 100));
					centerLoc.getWorld().spawnParticle(Particle.SNOWBALL, centerLoc, 10, 1, 1, 1, 0.01);
				} else {
					double precision = mRand.nextInt(50) + 100;
					double increment = (2 * Math.PI) / precision;
					double rad = (double)(mRadius * (w - mTime)) / 5;
					double angle = 0;
					for (int j = 0; j < precision; j++) {
						angle = j * increment;
						Location particleLoc = centerLoc.clone();
						particleLoc.setX(particleLoc.getX() + (rad * Math.cos(angle)));
						particleLoc.setZ(particleLoc.getZ() + (rad * Math.sin(angle)));
						particleLoc.getWorld().spawnParticle(Particle.SNOWBALL, particleLoc, 1, 0.02, 1.5 * rad, 0.02, 0);
					}

					if (w == mTime) {
						centerLoc.getWorld().playSound(centerLoc, Sound.ENTITY_WITHER_SHOOT, (float)mRadius / 7, 0.77F);
						centerLoc.getWorld().playSound(centerLoc, Sound.ENTITY_WITHER_SHOOT, (float)mRadius / 7, 0.5F);
						centerLoc.getWorld().playSound(centerLoc, Sound.ENTITY_WITHER_SHOOT, (float)mRadius / 7, 0.65F);
						for (Player player : Utils.playersInRange(mLauncher.getLocation(), mRadius)) {
							player.damage(12.0f);
							player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 8 * 20, 4));
						}
					}
				}

				w++;
				if (w >= mTime + 6) {
					this.cancel();
				}
			}
		};

		loop.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(loop);
	}

	@Override
	public int duration() {
		return 160; // 8 seconds
	}
}
