package com.playmonumenta.plugins.bosses.spells.lich.undeadplayers;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
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

/*
Undead Mage - Casts Magma shield every 10 second(s) dealing 30 damage in
a cone in front of it and setting players on fire for 5 seconds.
 */
public class SpellMagmaShield extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private PartialParticle mFlame1;
	private PartialParticle mFlame2;
	private PartialParticle mLava;
	private PartialParticle mSmoke;

	public SpellMagmaShield(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mFlame1 = new PartialParticle(Particle.FLAME, mBoss.getLocation(), 2, 0.3, 0.05, 0.3, 0.075);
		mFlame2 = new PartialParticle(Particle.FLAME, mBoss.getLocation(), 2, 0.15, 0.15, 0.15, 0.15);
		mLava = new PartialParticle(Particle.LAVA, mBoss.getLocation(), 1, 0.3, 0.05, 0.3, 0.075);
		mSmoke = new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation(), 2, 0.15, 0.15, 0.15, 0.1);
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0, false));
		world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.HOSTILE, 1, 0.8f);
		BukkitRunnable runA = new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				mT++;
				if (mT % 2 == 0) {
					world.playSound(mBoss.getLocation(), Sound.BLOCK_FIRE_AMBIENT, SoundCategory.HOSTILE, 1, 1.25f);
				}
				mFlame1.location(mBoss.getLocation()).spawnAsEnemy();
				mLava.location(mBoss.getLocation()).spawnAsEnemy();
				if (mT >= 20 * 2) {
					this.cancel();
					Location loc = mBoss.getLocation();
					Vector playerDir = mBoss.getEyeLocation().getDirection().setY(0).normalize();
					for (Player p : PlayerUtils.playersInRange(mBoss.getLocation(), 6, true)) {
						Vector toMobVector = p.getLocation().toVector().subtract(mBoss.getLocation().toVector()).setY(0).normalize();
						if (playerDir.dot(toMobVector) > 0.33) {
							MovementUtils.knockAway(mBoss, p, 0.5f, false);
							BossUtils.blockableDamage(mBoss, p, DamageType.MAGIC, 25, "Magma Shield", mBoss.getLocation());
							p.setFireTicks(20 * 5);
						}
					}
					BukkitRunnable runB = new BukkitRunnable() {
						double mRadius = 0;
						@Override
						public void run() {
							if (mRadius == 0) {
								loc.setDirection(mBoss.getLocation().getDirection().setY(0).normalize());
							}

							Vector vec;
							mRadius += 1.25;
							for (double degree = -60; degree <= 60; degree += 30) {
								double radian1 = Math.toRadians(degree);
								vec = new Vector(FastUtils.cos(radian1) * mRadius, 0.125, FastUtils.sin(radian1) * mRadius);
								vec = VectorUtils.rotateXAxis(vec, -loc.getPitch());
								vec = VectorUtils.rotateYAxis(vec, loc.getYaw() + 90);

								Location l = loc.clone().add(0, 0.1, 0).add(vec);
								mFlame2.location(l).spawnAsEnemy();
								mSmoke.location(l).spawnAsEnemy();
							}

							if (mRadius >= 7) {
								this.cancel();
							}
						}

					};
					runB.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(runB);
				}
			}

		};
		runA.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runA);
	}

	@Override
	public boolean canRun() {
		return PlayerUtils.playersInRange(mBoss.getLocation(), 9, true).size() > 0;
	}

	@Override
	public int cooldownTicks() {
		return 20 * 10;
	}

}
