package com.playmonumenta.bossfights.spells.spells_masked;

import java.util.EnumSet;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
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
		final int PERIOD = 3;


		BukkitRunnable loop = new BukkitRunnable() {
			final int DEFAULT_COUNTDOWN = 45 / PERIOD;
			final Location loc = mLauncher.getLocation();

			int countdown = DEFAULT_COUNTDOWN;
			int count = Math.min(MAX_BLAZES_PER_SPAWN, PlayerUtils.getNearbyPlayers(loc, Masked.DETECTION_RANGE).size());;
			int wavesLeft = SPAWN_CYCLES;

			@Override
			public void run() {
				Location centerLoc = loc.clone().add(0, 1, 0);
				mLauncher.teleport(loc);
				centerLoc.getWorld().playSound(centerLoc, Sound.BLOCK_PORTAL_AMBIENT, 4f, 2f);
				for (int j = 0; j < 5; j++) {
					Location particleLoc = centerLoc.clone().add(((double)(mRand.nextInt(4000) - 2000) / 1000),
					                                             ((double)(mRand.nextInt(4000) - 2000) / 1000),
																 ((double)(mRand.nextInt(4000) - 2000) / 1000));
					particleLoc.getWorld().spawnParticle(Particle.LAVA, particleLoc, 4, 0, 0, 0, 0.01);
				}

				countdown--;
				if (countdown <= 0) {
					for (int j = 0; j < count; j++) {
						Entity blaz = loc.getWorld().spawnEntity(loc, EntityType.BLAZE);
						double x = 0.5f * Math.cos((double)mRand.nextInt(628) / 100);
						double z = 0.5f * Math.sin((double)mRand.nextInt(628) / 100);
						blaz.setVelocity(new Vector(x, 0.3, z));
					}

					wavesLeft -= 1;
					if (wavesLeft <= 0) {
						this.cancel();
						return;
					}

					countdown = 2; // Spawn again 2 * PERIOD ticks later
				};
			}
		};

		loop.runTaskTimer(mPlugin, 0, PERIOD);
		mActiveRunnables.add(loop);
	}

	@Override
	public int duration() {
		return DURATION;
	}
}
