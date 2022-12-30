package com.playmonumenta.plugins.bosses.spells.imperialconstruct;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.ImperialConstruct;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellTemporalFlux extends Spell {
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private final int mRange = ImperialConstruct.detectionRange;
	private final int mCastTime = 20 * 3;
	private final int mBurstTime = 10;
	private final int mCooldown = 20 * 20;

	public static final String DEBUFF_NAME = "TemporalFlux";
	public static final int DURATION_TICKS = 20 * 25;

	public SpellTemporalFlux(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		// Get nearby players near boss and remove if they're a spectator.
		List<Player> players = EntityUtils.getNearestPlayers(mBoss.getLocation(), mRange);
		players.removeIf(p -> p.getGameMode() == GameMode.SPECTATOR);

		BukkitRunnable runnable = new BukkitRunnable() {
			private int mTicks = 0;
			final World mWorld = mBoss.getWorld();
			final int mBaseSize = 15;
			double mRate = 1;

			@Override
			public void run() {
				mTicks++;
				if (mTicks < mCastTime) {
					Location loc = mBoss.getLocation().add(0, 2, 0);
					mRate = (double) (mCastTime - mTicks) / mCastTime;
					double size = mBaseSize * mRate;

					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_CAT_HISS, SoundCategory.HOSTILE, 2f, mTicks / 10f);
					for (double i = 0; i <= Math.PI; i += Math.PI / 10) {
						double radius = Math.sin(i) * size;
						double y = Math.cos(i) * size;
						for (double a = 0; a < Math.PI * 2; a += Math.PI / 10) {
							double x = Math.cos(a) * radius;
							double z = Math.sin(a) * radius;
							new PartialParticle(Particle.SPELL_WITCH, loc.clone().add(x, y, z), 1, 0, 0, 0, 0).spawnAsEntityActive(mBoss);
						}
					}
				} else if (mTicks == mCastTime) {
					BukkitRunnable runnable2 = new BukkitRunnable() {
						final double mMaxRadius = 25;
						double mSecondaryRate;
						int mTicks = 0;

						@Override
						public void run() {
							mTicks++;
							mSecondaryRate = (double) mTicks / mBurstTime;
							double radius = mMaxRadius * mSecondaryRate;
							new PPCircle(Particle.FLAME, mBoss.getLocation().add(0, 1, 0), radius)
								.ringMode(true).count(36).spawnAsEntityActive(mBoss);

							if (mTicks >= mBurstTime) {
								this.cancel();
							}
						}

					};
					runnable2.runTaskTimer(mPlugin, 0, 2);
					mActiveRunnables.add(runnable2);

					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.HOSTILE, 10f, 1f);
					new PartialParticle(Particle.EXPLOSION_LARGE, mBoss.getLocation().add(0, 2, 0), 60, 1, 1, 1, 0.001).spawnAsEntityActive(mBoss);
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);

	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}

}
