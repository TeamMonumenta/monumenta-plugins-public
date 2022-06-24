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
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class StarfallBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_starfall";

	public static class Parameters extends BossParameters {
		public int COOLDOWN = 20 * 8;
		public int DELAY = 20 * 2;

		public int DAMAGE = 0;
		public DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.BLAST;
		public double DAMAGE_PERCENTAGE = 0.0;
		@BossParam(help = "The spell name shown when a player is killed by this skill")
		public String SPELL_NAME = "";
		public EffectsList EFFECTS = EffectsList.EMPTY;

		public int TRACKING = 20;

		public int LOCKING_DURATION = 20 * 2;
		public int METEOR_SPEED = 2;
		public double HEIGHT = 16;

		public double SPHERE_RADIUS = 4;

		public EntityTargets TARGETS = EntityTargets.GENERIC_ONE_PLAYER_TARGET;
		public EntityTargets TARGETS_EXPLOSION = EntityTargets.GENERIC_PLAYER_TARGET.clone().setRange(5);

		public ParticlesList PARTICLE_CIRCLE = ParticlesList.fromString("[(FLAME,1,0,0,0,0.1)]");

		public ParticlesList PARTICLE_METEOR = ParticlesList.fromString("[(FLAME,30,0.1,0.1,0.1,0.1),(SMOKE_LARGE,3)]");
		public ParticlesList PARTICLE_EXPLOSION = ParticlesList.fromString("[(FLAME,175,0.1,0.1,0.1,0.25),(SMOKE_LARGE,50,0,0,0,0.25),(EXPLOSION_NORMAL,50,0,0,0,0.25)]");

		public SoundsList SOUND_LOCKING = SoundsList.fromString("[(ITEM_FIRECHARGE_USE,1,0)]");
		public SoundsList SOUND_METEOR = SoundsList.fromString("[(ENTITY_BLAZE_SHOOT,3,1)]");
		public SoundsList SOUND_EXPLOSION = SoundsList.fromString("[(ENTITY_DRAGON_FIREBALL_EXPLODE,3,1)]");

	}


	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new StarfallBoss(plugin, boss);
	}

	public StarfallBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = Parameters.getParameters(boss, identityTag, new Parameters());
		SpellManager spellManager = new SpellManager(List.of(
			new Spell() {
				@Override
				public void run() {
					List<? extends LivingEntity> targets = p.TARGETS.getTargetsList(mBoss);

					for (LivingEntity target : targets) {
						new BukkitRunnable() {
							int mTicks = 0;
							Location mLocation = target.getLocation();


							@Override public void run() {
								if (mBoss.isDead() || !mBoss.isValid()) {
									cancel();
									return;
								}
								if (mTicks >= p.LOCKING_DURATION) {
									new BukkitRunnable() {
										final double mStep = p.METEOR_SPEED;
										double mCurrentHeight = p.HEIGHT;

										@Override
										public void run() {
											if (mBoss.isDead() || !mBoss.isValid()) {
												cancel();
												return;
											}

											mCurrentHeight -= mStep;
											Location meteorCenter = mLocation.clone().add(0, mCurrentHeight, 0);

											if (mCurrentHeight <= 0) {
												p.PARTICLE_EXPLOSION.spawn(boss, meteorCenter, p.TARGETS_EXPLOSION.getRange() / 2, p.TARGETS_EXPLOSION.getRange() / 2, p.TARGETS_EXPLOSION.getRange() / 2, 0.1);
												p.SOUND_EXPLOSION.play(meteorCenter);
												for (LivingEntity target : p.TARGETS_EXPLOSION.getTargetsListByLocation(mBoss, meteorCenter)) {

													if (p.DAMAGE > 0) {
														BossUtils.blockableDamage(mBoss, target, p.DAMAGE_TYPE, p.DAMAGE, p.SPELL_NAME, mBoss.getLocation());
													}

													if (p.DAMAGE_PERCENTAGE > 0.0) {
														BossUtils.bossDamagePercent(mBoss, target, p.DAMAGE_PERCENTAGE, p.SPELL_NAME);
													}
													p.EFFECTS.apply(target, mBoss);
												}

												cancel();
											}

											p.SOUND_METEOR.play(meteorCenter, 3.0f, (float) (mCurrentHeight / p.HEIGHT) * 1.5f);
											p.PARTICLE_METEOR.spawn(boss, meteorCenter);

										}
									}.runTaskTimer(mPlugin, 0, 1);

									cancel();
									return;
								}

								if (mTicks <= p.TRACKING) {
									mLocation = target.getLocation();
								}

								if (mTicks % 4 == 0) {
									double size = (p.LOCKING_DURATION - mTicks) / 20.0 * p.SPHERE_RADIUS;
									for (int degree = 0; degree <= 360; degree += 5) {
										double radiant = Math.toRadians(degree);
										Location l = mLocation.clone().add(FastUtils.cos(radiant) * size, 0.3, FastUtils.sin(radiant) * size);
										p.PARTICLE_CIRCLE.spawn(boss, l);
									}
								}

								p.SOUND_LOCKING.play(mLocation, 1, 1.5f * (((float) mTicks + 1)/(float) p.LOCKING_DURATION));


								mTicks += 2;
							}
						}.runTaskTimer(mPlugin, 0, 2);
					}

				}

				@Override public int cooldownTicks() {
					return p.COOLDOWN;
				}

				@Override public boolean canRun() {
					return !p.TARGETS.getTargetsList(mBoss).isEmpty();
				}
			}
		));

		super.constructBoss(spellManager, Collections.emptyList(), (int) (p.TARGETS.getRange() * 2), null, p.DELAY);
	}
}
