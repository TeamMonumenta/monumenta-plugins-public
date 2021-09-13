package com.playmonumenta.plugins.bosses.spells.lich.undeadplayers;

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

import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

/*
Undead Mage - Casts Magma shield every 10 second(s) dealing 30 damage in
a cone in front of it and setting players on fire for 5 seconds.
 */
public class SpellMagmaShield extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;

	public SpellMagmaShield(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
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
				new PartialParticle(Particle.FLAME, mBoss.getLocation(), 3, 0.3, 0.05, 0.3, 0.075).spawnAsEnemy();
				new PartialParticle(Particle.LAVA, mBoss.getLocation(), 1, 0.3, 0.05, 0.3, 0.075).spawnAsEnemy();
				if (mT >= 20 * 2) {
					this.cancel();
					Location loc = mBoss.getLocation();
					Vector playerDir = mBoss.getEyeLocation().getDirection().setY(0).normalize();
					for (Player p : Lich.playersInRange(mBoss.getLocation(), 6, true)) {
						Vector toMobVector = p.getLocation().toVector().subtract(mBoss.getLocation().toVector()).setY(0).normalize();
						if (playerDir.dot(toMobVector) > 0.33) {
							MovementUtils.knockAway(mBoss, p, 0.5f);
							BossUtils.bossDamage(mBoss, p, 30);
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
							for (double degree = -60; degree <= 60; degree += 10) {
								double radian1 = Math.toRadians(degree);
								vec = new Vector(FastUtils.cos(radian1) * mRadius, 0.125, FastUtils.sin(radian1) * mRadius);
								vec = VectorUtils.rotateXAxis(vec, -loc.getPitch());
								vec = VectorUtils.rotateYAxis(vec, loc.getYaw() + 90);

								Location l = loc.clone().add(0, 0.1, 0).add(vec);
								new PartialParticle(Particle.FLAME, l, 2, 0.15, 0.15, 0.15, 0.15).spawnAsEnemy();
								new PartialParticle(Particle.SMOKE_NORMAL, l, 3, 0.15, 0.15, 0.15, 0.1).spawnAsEnemy();
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
		return Lich.playersInRange(mBoss.getLocation(), 6, true).size() > 0;
	}

	@Override
	public int cooldownTicks() {
		return 20 * 10;
	}

}
