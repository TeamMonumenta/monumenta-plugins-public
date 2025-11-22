package com.playmonumenta.plugins.bosses.spells.lich.undeadplayers;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/*
 * Casts Smoke Bomb every 14 seconds giving players within 6 blocks weakness 2 and slowness 3 for 6 seconds.
 */

public class SpellSmokescreen extends Spell {
	private static final String SLOWNESS_SRC = "RogueRevenantSlowness";
	private static final String WEAKNESS_SRC = "RogueRevenantWeakness";

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int DELAY = (int) (20 * 1.25);
	private final int DURATION = 20 * 10;
	private final PartialParticle mSmokeN1;
	private final PartialParticle mSmokeN2;
	private final PartialParticle mSmokeL1;
	private final PartialParticle mSmokeL2;

	public SpellSmokescreen(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mSmokeN1 = new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation(), 3, 0.3, 0.05, 0.3, 0.075);
		mSmokeN2 = new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation(), 75, 3.5, 0.2, 4.5, 0.05);
		mSmokeL1 = new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 2, 0.3, 0.05, 0.3, 0.075);
		mSmokeL2 = new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 30, 3.5, 0.8, 4.5, 0.025);
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		Location loc = mBoss.getLocation();
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0, false));
		world.playSound(mBoss.getLocation(), Sound.ENTITY_CREEPER_PRIMED, SoundCategory.HOSTILE, 1.0f, 0.8f);
		BukkitRunnable run = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT == 0) {
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.75f, 1.2f);
				}
				mT += 10;
				mSmokeN1.location(loc).spawnAsEnemy();
				mSmokeL1.location(loc).spawnAsEnemy();
				if (mBoss.isDead()) {
					this.cancel();
					return;
				}
				if (mT >= DELAY) {
					mSmokeN2.location(loc.clone().add(0, 1, 0)).spawnAsEnemy();
					mSmokeL2.location(loc.clone().add(0, 1, 0)).spawnAsEnemy();
					world.playSound(mBoss.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.HOSTILE, 1, 0.7f);
					for (Player player : PlayerUtils.playersInRange(loc, 4, true)) {
						com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, SLOWNESS_SRC,
							new PercentSpeed(20 * 6, -0.5, SLOWNESS_SRC));
						com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, WEAKNESS_SRC,
							new PercentDamageDealt(20 * 6, -0.3));
					}
				}
				if (mT >= DELAY + DURATION) {
					this.cancel();
				}
			}

		};
		run.runTaskTimer(mPlugin, 20, 10);
		mActiveRunnables.add(run);
	}

	@Override
	public boolean canRun() {
		return !PlayerUtils.playersInRange(mBoss.getLocation(), 7, true).isEmpty();
	}

	@Override
	public int cooldownTicks() {
		return 20 * 14;
	}

}
