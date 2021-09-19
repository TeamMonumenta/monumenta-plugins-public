package com.playmonumenta.plugins.bosses.spells.lich.undeadplayers;

import java.util.Arrays;
import java.util.Collection;

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
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

/*
 * Every 12 second(s) deals 21 damage in a cone in front of it dealing an additional 7 damage per debuff on that player hit.
 */

public class SpellAmpHex extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Collection<PotionEffectType> mBadEffects = Arrays.asList(
			PotionEffectType.BLINDNESS,
			PotionEffectType.CONFUSION,
			PotionEffectType.HUNGER,
			PotionEffectType.LEVITATION,
			PotionEffectType.POISON,
			PotionEffectType.SLOW,
			PotionEffectType.SLOW_DIGGING,
			PotionEffectType.UNLUCK,
			PotionEffectType.WEAKNESS,
			PotionEffectType.WITHER
			);

	public SpellAmpHex(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		new PartialParticle(Particle.PORTAL, mBoss.getLocation(), 25, 0.1, 0.1, 0.1, 0.1).spawnAsEnemy();
		world.playSound(mBoss.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, SoundCategory.HOSTILE, 0.8f, 1.5f);

		BukkitRunnable runA = new BukkitRunnable() {

			@Override
			public void run() {
				BukkitRunnable run = new BukkitRunnable() {
					final Location mLoc = mBoss.getLocation();
					double mRadius = 0.5;

					@Override
					public void run() {
						if (mRadius == 0.5) {
							mLoc.setDirection(mBoss.getLocation().getDirection().setY(0).normalize());
						}

						Vector vec;
						mRadius += 1.25;
						for (double degree = 30; degree <= 120; degree += 10) {
							double radian1 = Math.toRadians(degree);
							vec = new Vector(Math.cos(radian1) * mRadius, 0.15, Math.sin(radian1) * mRadius);
							vec = VectorUtils.rotateXAxis(vec, -mLoc.getPitch());
							vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

							Location l = mLoc.clone().clone().add(0, 0.15, 0).add(vec);
							new PartialParticle(Particle.DRAGON_BREATH, l, 2, 0.05, 0.05, 0.05, 0.1).spawnAsEnemy();
							new PartialParticle(Particle.SMOKE_NORMAL, l, 1, 0.05, 0.05, 0.05, 0.1).spawnAsEnemy();
						}

						if (mRadius >= 9) {
							this.cancel();
						}
					}

				};
				run.runTaskTimer(mPlugin, 0, 1);
				mActiveRunnables.add(run);

				world.playSound(mBoss.getLocation(), Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.HOSTILE, 1.0f, 0.65f);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1.0f, 0.65f);

				Vector playerDir = mBoss.getEyeLocation().getDirection().setY(0).normalize();
				for (Player p : PlayerUtils.playersInRange(mBoss.getLocation(), 8, true)) {
					Vector toMobVector = p.getLocation().toVector().subtract(mBoss.getLocation().toVector()).setY(0).normalize();
					if (playerDir.dot(toMobVector) > 0.33) {
						int debuffCount = 0;
						for (PotionEffectType effectType: mBadEffects) {
							PotionEffect effect = p.getPotionEffect(effectType);
							if (effect != null) {
								debuffCount++;
							}
						}
						BossUtils.bossDamage(mBoss, p, 21 + 7 * debuffCount, mBoss.getLocation(), "Amplifying Hex");
						MovementUtils.knockAway(mBoss, p, 0.3f);
					}
				}
			}

		};
		runA.runTaskLater(mPlugin, 25);
		mActiveRunnables.add(runA);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 12;
	}

}
