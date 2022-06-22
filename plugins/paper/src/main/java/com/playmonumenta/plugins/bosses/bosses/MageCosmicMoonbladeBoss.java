package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class MageCosmicMoonbladeBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_mage_cosmic_moonblade";

	public static class Parameters extends BossParameters {
		public int COOLDOWN = 8 * 20;
		public int SWINGS = 3;

		@BossParam(help = "The spell name shown when a player is killed by this skill")
		public String SPELL_NAME = "";
		public int DAMAGE = 0;
		public double DAMAGE_PERCENTAGE = 0;
		public EffectsList EFFECTS = EffectsList.EMPTY;

		public int SPELL_DELAY = 20;
		public int SWINGS_DELAY = 10;
		public int DELAY = 4 * 20;
		public EntityTargets TARGETS_DIRECTION = EntityTargets.GENERIC_ONE_PLAYER_CLOSER_TARGET;
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET.clone().setRange(6);

		public ParticlesList PARTICLE_TELL = ParticlesList.fromString("[(REDSTONE,1,0,0,0,0.1,WHITE,1)]");

		public SoundsList SOUND_TELL = SoundsList.fromString("[(ENTITY_EVOKER_PREPARE_ATTACK,1,2)]");

		public ParticlesList PARTICLE_SWING = ParticlesList.fromString("[(REDSTONE,1,0,0,0,0.1,#6acbff,1),(REDSTONE,1,0,0,0,0.1,#a8e2ff,1)]");

		public SoundsList SOUND_SWING = SoundsList.fromString("[(ENTITY_PLAYER_ATTACK_SWEEP,0.75,0.8),(ENTITY_WITHER_SHOOT,0.75,0)]");
	}

	public static BossAbilityGroup deserialize(com.playmonumenta.plugins.Plugin plugin, LivingEntity boss) throws Exception {
		return new MageCosmicMoonbladeBoss(plugin, boss);
	}

	public final Parameters mParams;

	public MageCosmicMoonbladeBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = Parameters.getParameters(boss, identityTag, new Parameters());
		mParams = p;

		SpellManager spells = new SpellManager(List.of(
			new Spell() {
				@Override
				public void run() {
					boolean hasGlowing = mBoss.isGlowing();
					mBoss.setGlowing(true);
					List<? extends LivingEntity> targets = p.TARGETS_DIRECTION.getTargetsList(mBoss);

					if (targets.size() > 0) {
						Location bossEyeLoc = mBoss.getEyeLocation();
						Location targetLoc = targets.get(0).getLocation().clone().add(0, 1.8, 0);
						Vector targetDirection = targetLoc.toVector().subtract(bossEyeLoc.toVector()).normalize();
						Location originTargetDirection = mBoss.getLocation().clone().setDirection(targetDirection);

						mParams.SOUND_TELL.play(mBoss.getLocation());

						new BukkitRunnable() {
							int mDegree = 45;

							@Override public void run() {

								for (double r = 1; r < p.TARGETS.getRange(); r += 0.5) {
									for (double degree = mDegree; degree < mDegree + 30; degree += 5) {
										double radian1 = Math.toRadians(degree);
										Vector vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
										vec = VectorUtils.rotateZAxis(vec, -8);
										vec = VectorUtils.rotateXAxis(vec, originTargetDirection.getPitch());
										vec = VectorUtils.rotateYAxis(vec, originTargetDirection.getYaw());

										Location l = originTargetDirection.clone().add(0, 1.25, 0).add(vec);
										mParams.PARTICLE_TELL.spawn(l);
									}
								}
								if (mDegree >= 135) {
									cancel();
									return;
								}
								mDegree += 30;
							}
						}.runTaskTimer(mPlugin, 0, 1);


						new BukkitRunnable() {
							final Vector mTargetDirection = targetDirection;
							int mTimes = 0;
							float mPitch = 1.2f;
							int mSwings = 0;

							@Override
							public void run() {
								if (mTimes == 0 && !hasGlowing) {
									mBoss.setGlowing(false);
								}

								if (!mBoss.isValid() || mBoss.isDead()) {
									cancel();
									return;
								}
								mTimes++;
								mSwings++;
								Location origin = mBoss.getLocation().clone();
								Location originTargetDirection = origin.setDirection(mTargetDirection);
								List<? extends LivingEntity> targets = p.TARGETS.getTargetsList(mBoss);

								if (mTimes >= p.SWINGS) {
									mPitch = 1.45f;
								}
								mParams.SOUND_SWING.play(originTargetDirection, 1, mPitch);

								new BukkitRunnable() {
									final int mI = mSwings;
									double mRoll;
									double mD = 45;
									boolean mInit = false;
									final List<LivingEntity> mAlreadyHit = new ArrayList<>();

									@Override
									public void run() {
										if (!mInit) {
											if (mI % 2 == 0) {
												mRoll = -8;
												mD = 45;
											} else {
												mRoll = 8;
												mD = 135;
											}
											mInit = true;
										}
										if (!mBoss.isValid() || mBoss.isDead()) {
											cancel();
											return;
										}
										if (mI % 2 == 0) {
											Vector vec;
											for (double r = 1; r < p.TARGETS.getRange(); r += 0.5) {
												for (double degree = mD; degree < mD + 30; degree += 5) {
													double radian1 = Math.toRadians(degree);
													vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
													vec = VectorUtils.rotateZAxis(vec, mRoll);
													vec = VectorUtils.rotateXAxis(vec, originTargetDirection.getPitch());
													vec = VectorUtils.rotateYAxis(vec, originTargetDirection.getYaw());

													Location l = originTargetDirection.clone().add(0, 1.25, 0).add(vec);
													mParams.PARTICLE_SWING.spawn(l);

													BoundingBox box = BoundingBox.of(l, 0.3, 1, 0.3);

													for (LivingEntity target : targets) {
														if (target.getBoundingBox().overlaps(box) && !mAlreadyHit.contains(target)) {
															damageAction(target);
															mAlreadyHit.add(target);
														}
													}
												}
											}

											mD += 30;
										} else {
											Vector vec;
											for (double r = 1; r < p.TARGETS.getRange(); r += 0.5) {
												for (double degree = mD; degree > mD - 30; degree -= 5) {
													double radian1 = Math.toRadians(degree);
													vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
													vec = VectorUtils.rotateZAxis(vec, mRoll);
													vec = VectorUtils.rotateXAxis(vec, originTargetDirection.getPitch());
													vec = VectorUtils.rotateYAxis(vec, originTargetDirection.getYaw());

													Location l = originTargetDirection.clone().add(0, 1.25, 0).add(vec);
													l.setPitch(-l.getPitch());
													mParams.PARTICLE_SWING.spawn(l);

													BoundingBox box = BoundingBox.of(l, 0.3, 1, 0.3);

													for (LivingEntity target : targets) {
														if (target.getBoundingBox().overlaps(box) && !mAlreadyHit.contains(target)) {
															damageAction(target);
															mAlreadyHit.add(target);
														}
													}
												}
											}
											mD -= 30;
										}

										if ((mD >= 135 && mI % 2 == 0) || (mD <= 45 && mI % 2 > 0)) {
											mAlreadyHit.clear();
											this.cancel();
										}
									}

								}.runTaskTimer(mPlugin, 0, 1);
								if (mTimes >= p.SWINGS) {
									this.cancel();
								}
							}
						}.runTaskTimer(mPlugin, p.SPELL_DELAY, p.SWINGS_DELAY);
					}
				}

				@Override
				public boolean canRun() {
					return p.TARGETS_DIRECTION.getTargetsList(mBoss).size() > 0;
				}

				@Override
				public int cooldownTicks() {
					return p.COOLDOWN;
				}
			}
		));

		super.constructBoss(spells, Collections.emptyList(), (int) p.TARGETS.getRange(), null, p.DELAY);
	}

	public void damageAction(LivingEntity target) {
		if (mParams.DAMAGE > 0) {
			BossUtils.blockableDamage(mBoss, target, DamageEvent.DamageType.MAGIC, mParams.DAMAGE, mParams.SPELL_NAME, mBoss.getLocation());
		}

		if (mParams.DAMAGE_PERCENTAGE > 0.0) {
			BossUtils.bossDamagePercent(mBoss, target, mParams.DAMAGE_PERCENTAGE, mParams.SPELL_NAME);
		}

		mParams.EFFECTS.apply(target, mBoss);
	}
}
