package com.playmonumenta.plugins.infinitytower.mobs.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;


public class AutoAttackTowerAbility extends TowerAbility {

	private static final int TICKS = 20 * 3;
	private static final double DISTANCE_MELEE_ATK = 6;
	private static final int MELEE_DAMAGE = 15;
	private static final int RANGED_DAMAGE = 12;

	private static final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f);
	private static final Particle.DustOptions YELLOW = new Particle.DustOptions(Color.fromRGB(255, 255, 0), 1.0f);

	public AutoAttackTowerAbility(Plugin plugin, String identityTag, LivingEntity boss, TowerGame game, TowerMob mob, boolean isPlayerMob) {
		super(plugin, identityTag, boss, game, mob, isPlayerMob);

		Spell spell = new Spell() {

			private double mInc = 0;
			@Override
			public void run() {
				mInc += 5;

				if (mInc >= TICKS) {
					mInc -= TICKS;
					cast();
				}
			}

			private void cast() {
				List<LivingEntity> targets = (mIsPlayerMob ? mGame.getFloorMobs() : mGame.getPlayerMobs());
				for (LivingEntity target : targets) {
					if (target.getLocation().distance(mBoss.getLocation()) <= DISTANCE_MELEE_ATK) {
						meleeAtk(target, targets);
						return;
					}
				}

				Collections.shuffle(targets);
				for (LivingEntity target : targets) {
					rangedAtk(target, targets);
					return;
				}
			}

			@Override
			public int cooldownTicks() {
				return 5;
			}


			public void meleeAtk(LivingEntity target, List<LivingEntity> targets) {
				Vector dir = LocationUtils.getDirectionTo(target.getLocation(), mBoss.getLocation());
				Location tloc = mBoss.getLocation().setDirection(dir);

				BukkitRunnable runB = new BukkitRunnable() {
					int mT = 0;
					private final PartialParticle mPRed = new PartialParticle(Particle.REDSTONE, mBoss.getLocation(), 2, 0.1, 0.1, 0.1, 0.1, RED);
					private final PartialParticle mPWitch = new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation(), 1, 0.1, 0.1, 0.1, 0.1);
					private final PartialParticle mPCrit1 = new PartialParticle(Particle.CRIT_MAGIC, mBoss.getLocation(), 2, 0.1, 0.1, 0.1, 0.1);
					private final PartialParticle mPCrit2 = new PartialParticle(Particle.CRIT_MAGIC, mBoss.getLocation(), 20, 0.25, 0.25, 0.25, 0.25);
					private final World mWorld = mBoss.getWorld();

					@Override
					public void run() {
						mT++;
						Vector v;
						for (double r = 0; r <= 5; r += 0.75) {
							for (double degree = -40; degree < 40; degree += 10) {
								double radian1 = Math.toRadians(degree);
								v = new Vector(Math.cos(radian1) * r, 0, Math.sin(radian1) * r);
								v = VectorUtils.rotateXAxis(v, 0);
								v = VectorUtils.rotateYAxis(v, tloc.getYaw() + 90);

								Location loc = mBoss.getLocation().clone().add(v);
								mPRed.location(loc).spawnAsBoss();
							}
						}
						if (mT >= 10) {
							Vector vec;
							for (double r1 = 0; r1 <= 5; r1 += 0.75) {
								for (double degree1 = -40; degree1 < 40; degree1 += 10) {
									double radian2 = Math.toRadians(degree1);
									vec = new Vector(Math.cos(radian2) * r1, 0, Math.sin(radian2) * r1);
									vec = VectorUtils.rotateXAxis(vec, 0);
									vec = VectorUtils.rotateYAxis(vec, tloc.getYaw() + 90);

									Location l = mBoss.getLocation().clone().add(vec);
									mPWitch.location(l).spawnAsBoss();
									mPCrit1.location(l).spawnAsBoss();
									BoundingBox box = BoundingBox.of(l, 0.4, 10, 0.4);

									for (LivingEntity target : targets) {
										if (target.getBoundingBox().overlaps(box)) {
											MovementUtils.knockAway(mBoss.getLocation(), target, 1.5f, 0.5f);
											mPCrit2.location(target.getLocation()).spawnAsBoss();
											DamageUtils.damage(mBoss, target, DamageEvent.DamageType.MAGIC, MELEE_DAMAGE);
										}
									}
								}
							}
							mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 1.0f);
							mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 3.0f, 1.0f);
							mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 3.0f, 0.5f);
							this.cancel();
						}
					}
				};
				runB.runTaskTimer(mPlugin, 0, 2);
				mActiveRunnables.add(runB);
			}


			public void rangedAtk(LivingEntity target, List<LivingEntity> targets) {
				BukkitRunnable runC = new BukkitRunnable() {
					private final BoundingBox mBox = BoundingBox.of(mBoss.getEyeLocation(), 0.3, 0.3, 0.3);
					private final PartialParticle mPSmoke2 = new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 15, 0, 0, 0, 0.25);
					private final PartialParticle mPYellow = new PartialParticle(Particle.REDSTONE, mBoss.getLocation(), 15, 0.2, 0.2, 0.2, 0.25, YELLOW);
					private final PartialParticle mPSoul = new PartialParticle(Particle.SOUL_FIRE_FLAME, mBoss.getLocation(), 2, 0.35, 0.35, 0.35, 0.025);
					private final PartialParticle mPWitch2 = new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation(), 4, 0.2, 0.2, 0.2, 0.125);
					private final PartialParticle mPYellow2 = new PartialParticle(Particle.REDSTONE, mBoss.getLocation(), 2, 0.2, 0.2, 0.2, 0.25, YELLOW);

					int mInnerTicks = 0;

					@Override
					public void run() {
						World w = mBoss.getWorld();
						Vector dir = LocationUtils.getDirectionTo(target.getLocation().add(0, 1, 0), mBoss.getEyeLocation());
						// Iterate two times and half the velocity so that way we can have more accurate travel for intersection.
						for (int j = 0; j < 2; j++) {
							mBox.shift(dir.clone().multiply(0.9 * 0.5));
							Location loc = mBox.getCenter().toLocation(mBoss.getWorld());
							for (LivingEntity target2 : targets) {
								if (target2.getBoundingBox().overlaps(mBox)) {
									DamageUtils.damage(mBoss, target, DamageEvent.DamageType.MAGIC, RANGED_DAMAGE);
									if (!plugin.mEffectManager.hasEffect(target, "ITPercentDamageReceived")) {
										plugin.mEffectManager.addEffect(target, "ITPercentDamageReceived",
											new PercentDamageReceived(20 * 10, 0.1));
									} else {
										double oldAmount = plugin.mEffectManager.getEffects(target, "ITPercentDamageReceived").last().getMagnitude();
										double newAmount = oldAmount + 0.1;
										plugin.mEffectManager.addEffect(target, "ITPercentDamageReceived",
											new PercentDamageReceived(20 * 10, newAmount));
									}

									mPSmoke2.location(loc).spawnAsBoss();
									mPYellow.location(loc).spawnAsBoss();
									w.playSound(loc, Sound.ENTITY_WITHER_HURT, 1, 0.75f);
									this.cancel();
								}
							}

							if (loc.getBlock().getType().isSolid()) {
								mPSmoke2.location(loc).spawnAsBoss();
								mPYellow.location(loc).spawnAsBoss();
								w.playSound(loc, Sound.ENTITY_WITHER_HURT, 1, 0.75f);
								this.cancel();
							}
						}
						Location loc = mBox.getCenter().toLocation(mBoss.getWorld());
						mPSoul.location(loc).spawnAsBoss();
						mPWitch2.location(loc).spawnAsBoss();
						mPYellow2.location(loc).spawnAsBoss();

						mInnerTicks++;
						if (mInnerTicks >= 20 * 5 || !mBoss.isValid() || mGame.isTurnEnded() || mBoss.isDead()) {
							this.cancel();
						}
					}
				};
				runC.runTaskTimer(mPlugin, 0, 1);
				mActiveRunnables.add(runC);
			}
		};


		SpellManager manager = new SpellManager(List.of(spell));

		super.constructBoss(manager, Collections.emptyList(), -1, null, (int) (FastUtils.RANDOM.nextDouble() * 20) + 20);

	}



}
