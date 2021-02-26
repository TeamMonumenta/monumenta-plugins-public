package com.playmonumenta.plugins.bosses.spells.masked;

import java.util.EnumSet;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.bosses.Masked;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellSummonBlazes extends Spell {

	private static final int MAX_NEARBY_BLAZES_MULTIPLIER = 4;
	private static final int MAX_BLAZES_PER_SPAWN = 4;
	private static final int SPAWN_CYCLES = 2;
	private static final int DURATION = 20 * 8;
	private static final int PERIOD = 3;
	private static final int DEFAULT_COUNTDOWN = 45 / PERIOD;

	private Plugin mPlugin;
	private Entity mLauncher;

	public SpellSummonBlazes(Plugin plugin, Entity launcher) {
		mPlugin = plugin;
		mLauncher = launcher;
	}

	// Only run if there are fewer blazes than the multiplier * # of players.
	@Override
	public boolean canRun() {
		return EntityUtils.getNearbyMobs(mLauncher.getLocation(), Masked.DETECTION_RANGE, EnumSet.of(EntityType.BLAZE)).size()
		       < PlayerUtils.playersInRange(mLauncher.getLocation(), Masked.DETECTION_RANGE).size() * MAX_NEARBY_BLAZES_MULTIPLIER;
	}

	@Override
	public void run() {


		BukkitRunnable loop = new BukkitRunnable() {
			final Location mLoc = mLauncher.getLocation();

			int mCountdown = DEFAULT_COUNTDOWN;
			int mCount = Math.min(MAX_BLAZES_PER_SPAWN, PlayerUtils.playersInRange(mLoc, Masked.DETECTION_RANGE).size());
			int mWavesLeft = SPAWN_CYCLES;

			@Override
			public void run() {
				Location centerLoc = mLoc.clone().add(0, 1, 0);
				mLauncher.teleport(mLoc);
				centerLoc.getWorld().playSound(centerLoc, Sound.BLOCK_PORTAL_AMBIENT, 4f, 2f);
				for (int j = 0; j < 5; j++) {
					Location particleLoc = centerLoc.clone().add(((double)(FastUtils.RANDOM.nextInt(4000) - 2000) / 1000),
					                                             ((double)(FastUtils.RANDOM.nextInt(4000) - 2000) / 1000),
																 ((double)(FastUtils.RANDOM.nextInt(4000) - 2000) / 1000));
					particleLoc.getWorld().spawnParticle(Particle.LAVA, particleLoc, 4, 0, 0, 0, 0.01);
				}

				mCountdown--;
				if (mCountdown <= 0) {
					for (int j = 0; j < mCount; j++) {
						Entity blaz = mLoc.getWorld().spawnEntity(mLoc, EntityType.BLAZE);
						double x = 0.5f * FastUtils.cos((double)FastUtils.RANDOM.nextInt(628) / 100);
						double z = 0.5f * FastUtils.sin((double)FastUtils.RANDOM.nextInt(628) / 100);
						blaz.setVelocity(new Vector(x, 0.3, z));
					}

					mWavesLeft -= 1;
					if (mWavesLeft <= 0) {
						this.cancel();
						return;
					}

					mCountdown = 2; // Spawn again 2 * PERIOD ticks later
				}
			}
		};

		loop.runTaskTimer(mPlugin, 0, PERIOD);
		mActiveRunnables.add(loop);
	}

	@Override
	public int cooldownTicks() {
		return DURATION;
	}
}
