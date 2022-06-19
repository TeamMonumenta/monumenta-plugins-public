package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class WarriorShieldWallBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_warrior_shield_wall";

	public static class Parameters extends BossParameters {
		public int COOLDOWN = 16 * 20;
		public int DELAY = 4 * 20;
		public int DURATION = 8 * 20;

		public int RADIUS = 180;
		public int DISTANCE = 4;

		public String SPELL_NAME = "";

		public float KNOCK_BACK = 0.3f;
		public DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.MAGIC;
		public int DAMAGE = 0;
		public double DAMAGE_PERCENTAGE = 0.0;
		public EffectsList EFFECTS = EffectsList.EMPTY;

		public boolean CAN_BLOCK_PROJECTILE = true;

		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET;

		public ParticlesList PARTICLE_CAST = ParticlesList.fromString("[(FIREWORKS_SPARK,70,0,0,0,0.3)]");

		public ParticlesList PARTICLE_WALL = ParticlesList.fromString("[(REDSTONE,1,0.1,0.2,0.1,0.0,GRAY,1),(SPELL_INSTANT)]");

		public ParticlesList PARTICLE_DEFLECT_PROJECTILE = ParticlesList.fromString("[(FIREWORKS_SPARK,5,0,0,0,0.25)]");

		public ParticlesList PARTICLE_DEFLECT_ENTITY = ParticlesList.fromString("[(EXPLOSION_NORMAL,30,0,0,0,0.35)]");

		public SoundsList SOUND_CAST = SoundsList.fromString("[(BLOCK_ANVIL_PLACE,1,1.5),(ENTITY_IRON_GOLEM_HURT,1,0.8)]");

		public SoundsList SOUND_DEFLECT_PROJECTILE = SoundsList.fromString("[(ENTITY_ZOMBIE_ATTACK_IRON_DOOR,0.75,1.5)]");
		public SoundsList SOUND_DEFLECT_ENTITY = SoundsList.fromString("[(ENTITY_GENERIC_EXPLODE,1,1)]");

	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new WarriorShieldWallBoss(plugin, boss);
	}

	public WarriorShieldWallBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = Parameters.getParameters(boss, identityTag, new Parameters());

		SpellManager spells = new SpellManager(List.of(
			new Spell() {

				@Override
				public void run() {
					p.PARTICLE_CAST.spawn(mBoss.getLocation());
					p.SOUND_CAST.play(mBoss.getLocation());
					new BukkitRunnable() {
						int mT = 0;
						final Location mLoc = mBoss.getLocation();
						final World mWorld = mBoss.getWorld();
						final List<BoundingBox> mBoxes = new ArrayList<>();
						List<LivingEntity> mMobsAlreadyHit = new ArrayList<>();
						final List<LivingEntity> mMobsHitThisTick = new ArrayList<>();
						boolean mHitboxes = false;

						@Override
						public void run() {
							mT++;
							Vector vec;
							for (int y = 0; y < 5; y++) {
								for (double degree = 0; degree < p.RADIUS; degree += 10) {
									double radian1 = Math.toRadians(degree);
									vec = new Vector(FastUtils.cos(radian1) * p.DISTANCE, y, FastUtils.sin(radian1) * p.DISTANCE);
									vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

									Location l = mLoc.clone().add(vec);
									if (mT % 4 == 0) {
										p.PARTICLE_WALL.spawn(l);
									}
									if (!mHitboxes) {
										mBoxes.add(BoundingBox.of(l.clone().subtract(0.6, 0, 0.6),
											l.clone().add(0.6, 5, 0.6)));
									}
								}
								mHitboxes = true;
							}

							List<LivingEntity> targets = (List<LivingEntity>) p.TARGETS.getTargetsList(mBoss);
							for (BoundingBox box : mBoxes) {
								for (Entity e : mLoc.getWorld().getNearbyEntities(box)) {
									Location eLoc = e.getLocation();
									if (e instanceof Projectile proj && p.CAN_BLOCK_PROJECTILE && proj.getShooter() instanceof LivingEntity livingEntity) {
										if (targets.contains(livingEntity)) {
											proj.remove();
											p.PARTICLE_DEFLECT_PROJECTILE.spawn(eLoc);
											p.SOUND_DEFLECT_PROJECTILE.play(eLoc);
										}
									} else if (e instanceof LivingEntity le && targets.contains(le)) {
										// Stores mobs hit this tick
										mMobsHitThisTick.add(le);
										// This list does not update to the mobs hit this tick until after everything runs
										if (!mMobsAlreadyHit.contains(le)) {
											mMobsAlreadyHit.add(le);

											if (p.DAMAGE > 0) {
												BossUtils.blockableDamage(boss, le, p.DAMAGE_TYPE, p.DAMAGE, p.SPELL_NAME, mBoss.getLocation());
											}

											if (p.DAMAGE_PERCENTAGE > 0) {
												BossUtils.bossDamagePercent(mBoss, le, p.DAMAGE_PERCENTAGE, box.getCenter().toLocation(mWorld), p.SPELL_NAME);
											}

											p.EFFECTS.apply(le, boss);

											if (p.KNOCK_BACK != 0) {
												MovementUtils.knockAway(mLoc, le, p.KNOCK_BACK, true);
											}
											p.PARTICLE_DEFLECT_ENTITY.spawn(eLoc);
											p.SOUND_DEFLECT_ENTITY.play(eLoc);
										}

									}
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
								if (mMobsHitThisTick.contains(mob)) {
									mobsAlreadyHitAdjusted.add(mob);
								}
							}
							mMobsAlreadyHit = mobsAlreadyHitAdjusted;
							mMobsHitThisTick.clear();
							if (mT >= p.DURATION) {
								this.cancel();
								mBoxes.clear();
							}

							if (!mBoss.isValid() || mBoss.isDead()) {
								cancel();
								mBoxes.clear();
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
				}

				@Override
				public int cooldownTicks() {
					return p.COOLDOWN;
				}
			}
		));

		super.constructBoss(spells, Collections.emptyList(), (int) p.TARGETS.getRange(), null, p.DELAY);

	}
}
