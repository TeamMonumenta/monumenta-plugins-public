package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class WarriorShieldWallBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_warrior_shield_wall";

	public static class Parameters extends BossParameters {
		public int COOLDOWN = 16 * 20;
		public int DELAY = 4 * 20;
		public int DURATION = 8 * 20;

		@BossParam(help = "This parameter actually controls the ANGLE in degrees")
		public int RADIUS = 180;
		public float DISTANCE = 4f;
		public int HEIGHT = 5;

		public String SPELL_NAME = "";

		public float KNOCK_BACK = 0.3f;
		public DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.MAGIC;
		public int DAMAGE = 0;
		public double DAMAGE_PERCENTAGE = 0.0;
		public EffectsList EFFECTS = EffectsList.EMPTY;

		public boolean CAN_BLOCK_PROJECTILE = true;

		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET;

		public ParticlesList PARTICLE_CAST = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.FIREWORKS_SPARK, 70, 0.0, 0.0, 0.0, 0.3))
			.build();

		public ParticlesList PARTICLE_WALL = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.REDSTONE, 1, 0.1, 0.2, 0.1, 0.0, new Particle.DustOptions(Color.GRAY, 1.0f)))
			.add(new ParticlesList.CParticle(Particle.SPELL_INSTANT, 1, 0.0, 0.0, 0.0, 0.0))
			.build();

		public ParticlesList PARTICLE_DEFLECT_PROJECTILE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.FIREWORKS_SPARK, 5, 0.0, 0.0, 0.0, 0.25))
			.build();

		public ParticlesList PARTICLE_DEFLECT_ENTITY = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.EXPLOSION_NORMAL, 30, 0.0, 0.0, 0.0, 0.35))
			.build();

		public SoundsList SOUND_CAST = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.5f))
			.add(new SoundsList.CSound(Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.8f))
			.build();

		public SoundsList SOUND_DEFLECT_PROJECTILE = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.75f, 1.5f))
			.build();
		public SoundsList SOUND_DEFLECT_ENTITY = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f))
			.build();

	}

	public WarriorShieldWallBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = Parameters.getParameters(boss, identityTag, new Parameters());

		Spell spell = new Spell() {

			@Override
			public void run() {
				if (EntityUtils.shouldCancelSpells(mBoss)) {
					return;
				}

				p.PARTICLE_CAST.spawn(mBoss, mBoss.getLocation());
				p.SOUND_CAST.play(mBoss.getLocation());
				new BukkitRunnable() {
					int mT = 0;
					final Location mLoc = mBoss.getLocation();
					final Hitbox mHitbox = Hitbox.approximateHollowCylinderSegment(mLoc, p.HEIGHT, p.DISTANCE - 0.6, p.DISTANCE + 0.6, Math.toRadians(p.RADIUS) / 2);
					List<LivingEntity> mMobsAlreadyHit = new ArrayList<>();

					@Override
					public void run() {
						mT++;
						Vector vec;

						if (mT % 4 == 0) {
							for (double degree = 0; degree < p.RADIUS; degree += 10) {
								double radian1 = Math.toRadians(degree);
								vec = new Vector(FastUtils.cos(radian1) * p.DISTANCE, 0, FastUtils.sin(radian1) * p.DISTANCE);
								vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

								Location l = mLoc.clone().add(vec);
								for (int y = 0; y < p.HEIGHT; y++) {
									l.add(0, 1, 0);
									p.PARTICLE_WALL.spawn(boss, l);
								}
							}
						}

						List<? extends LivingEntity> targets = p.TARGETS.getTargetsList(mBoss);

						if (p.CAN_BLOCK_PROJECTILE) {
							List<Projectile> projectiles = mHitbox.getHitEntitiesByClass(Projectile.class);
							for (Projectile proj : projectiles) {
								if (proj.getShooter() instanceof LivingEntity shooter && targets.contains(shooter)) {
									proj.remove();
									Location projLoc = proj.getLocation();
									p.PARTICLE_DEFLECT_PROJECTILE.spawn(boss, projLoc);
									p.SOUND_DEFLECT_PROJECTILE.play(projLoc);
								}
							}
						}

						List<LivingEntity> entities = targets.stream().filter(e -> mHitbox.intersects(e.getBoundingBox())).collect(Collectors.toList());
						for (LivingEntity le : entities) {
							// This list does not update to the mobs hit this tick until after everything runs
							if (!mMobsAlreadyHit.contains(le)) {
								mMobsAlreadyHit.add(le);

								Location shieldLocation = mLoc.clone();
								shieldLocation.setY(le.getEyeLocation().getY());
								if (le.getEyeLocation().distanceSquared(shieldLocation) < p.DISTANCE * p.DISTANCE) {
									shieldLocation.add(LocationUtils.getDirectionTo(le.getEyeLocation(), shieldLocation).multiply(p.DISTANCE));
								}

								if (p.DAMAGE > 0) {
									BossUtils.blockableDamage(boss, le, p.DAMAGE_TYPE, p.DAMAGE, p.SPELL_NAME, shieldLocation, p.EFFECTS.mEffectList());
								}

								if (p.DAMAGE_PERCENTAGE > 0) {
									DamageUtils.damagePercentHealth(mBoss, le, p.DAMAGE_PERCENTAGE, true,
										true, p.SPELL_NAME, true, p.EFFECTS.mEffectList());
								}

								p.EFFECTS.apply(le, boss);

								if (p.KNOCK_BACK != 0) {
									MovementUtils.knockAway(mLoc, le, p.KNOCK_BACK, true);
								}

								Location entityLoc = le.getLocation();
								p.PARTICLE_DEFLECT_ENTITY.spawn(boss, entityLoc);
								p.SOUND_DEFLECT_ENTITY.play(entityLoc);
							}
						}

						/*
						 * Compare the two lists of mobs and only remove from the
						 * actual hit tracker if the mob isn't detected as hit this
						 * tick, meaning it is no longer in the shield wall hitbox
						 * and is thus eligible for another hit.
						 */
						List<LivingEntity> mobsAlreadyHitAdjusted = new ArrayList<>();
						for (LivingEntity mob : mMobsAlreadyHit) {
							if (entities.contains(mob)) {
								mobsAlreadyHitAdjusted.add(mob);
							}
						}
						mMobsAlreadyHit = mobsAlreadyHitAdjusted;
						if (mT >= p.DURATION || EntityUtils.shouldCancelSpells(mBoss)) {
							this.cancel();
						}
					}

				}.runTaskTimer(mPlugin, 0, 1);
			}

			@Override
			public int cooldownTicks() {
				return p.COOLDOWN;
			}
		};

		super.constructBoss(spell, (int) p.TARGETS.getRange(), null, p.DELAY);

	}
}
