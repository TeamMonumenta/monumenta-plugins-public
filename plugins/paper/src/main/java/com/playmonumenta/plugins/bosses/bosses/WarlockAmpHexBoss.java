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
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class WarlockAmpHexBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_warlock_amp_hex";

	private static final Collection<PotionEffectType> BAD_EFFECTS = Arrays.asList(
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

	public static class Parameters extends BossParameters {
		public int COOLDOWN = 20 * 8;
		public int DELAY = 20 * 2;
		public int SPELL_DELAY = (int) (20 * 1.5);

		public int CONE = 40;

		public EffectsList EFFECTS = EffectsList.EMPTY;
		public DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.MAGIC;
		public int BASE_DAMAGE = 0;
		public int DAMAGE_PER_DEBUFF = 0;
		public double DAMAGE_PERCENTAGE = 0.0;
		@BossParam(help = "The spell name shown when a player is killed by this skill")
		public String SPELL_NAME = "";

		public EntityTargets TARGETS = EntityTargets.GENERIC_ONE_PLAYER_TARGET.clone().setRange(8).setLimit(EntityTargets.Limit.DEFAULT_CLOSER);

		public ParticlesList PARTICLE_CONE_WARNING = ParticlesList.fromString("[(REDSTONE,2,0.1,0.1,0.1,0.1,RED,1)]");
		public ParticlesList PARTICLE_CONE_HIT = ParticlesList.fromString("[(SPELL_WITCH,1,0.1,0.1,0.1,0.1),(CRIT_MAGIC,2,0.1,0.1,0.1,0.1)]");
		public ParticlesList PARTICLE_HIT = ParticlesList.fromString("[(CRIT_MAGIC,20,0.25,0.25,0.25,0.25)]");

		public SoundsList SOUND_CAST = SoundsList.fromString("[(ENTITY_ZOMBIE_BREAK_WOODEN_DOOR,1,1),(ENTITY_BLAZE_SHOOT,3,1),(BLOCK_END_PORTAL_FRAME_FILL,3,0.5)]");
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new WarlockAmpHexBoss(plugin, boss);
	}

	public WarlockAmpHexBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = Parameters.getParameters(boss, identityTag, new Parameters());

		SpellManager spell = new SpellManager(
			List.of(
				new Spell() {
					@Override
					public void run() {
						boolean hasGlowing = mBoss.isGlowing();
						mBoss.setGlowing(true);

						List<? extends LivingEntity> targets = p.TARGETS.getTargetsList(mBoss);
						Vector dir = LocationUtils.getDirectionTo(targets.get(0).getLocation(), mBoss.getLocation());
						Location tloc = mBoss.getLocation().setDirection(dir);

						BukkitRunnable runB = new BukkitRunnable() {
							int mT = 0;

							@Override
							public void run() {
								mT++;
								if (mT % 2 == 0) {
									Vector v;
									for (double r = 0; r <= p.TARGETS.getRange(); r += 0.5) {
										for (double degree = -p.CONE; degree < p.CONE; degree += 10) {
											double radian1 = Math.toRadians(degree);
											v = new Vector(Math.cos(radian1) * r, 0.8, Math.sin(radian1) * r);
											v = VectorUtils.rotateXAxis(v, 0);
											v = VectorUtils.rotateYAxis(v, tloc.getYaw() + 90);

											Location loc = mBoss.getLocation().clone().add(v);
											p.PARTICLE_CONE_WARNING.spawn(boss, loc);
										}
									}
								}
								if (mT >= p.SPELL_DELAY) {
									if (!hasGlowing) {
										mBoss.setGlowing(false);
									}
									Vector vec;
									for (double r1 = 0; r1 <= p.TARGETS.getRange(); r1 += 0.5) {
										for (double degree1 = -p.CONE; degree1 < p.CONE; degree1 += 10) {
											double radian2 = Math.toRadians(degree1);
											vec = new Vector(Math.cos(radian2) * r1, 0.8, Math.sin(radian2) * r1);
											vec = VectorUtils.rotateXAxis(vec, 0);
											vec = VectorUtils.rotateYAxis(vec, tloc.getYaw() + 90);

											Location l = mBoss.getLocation().clone().add(vec);
											p.PARTICLE_CONE_HIT.spawn(boss, l);
											BoundingBox box = BoundingBox.of(l, 0.4, 2, 0.4);

											for (LivingEntity target : targets) {
												if (target.getBoundingBox().overlaps(box)) {
													int debuffCount = 0;
													for (PotionEffectType effectType : BAD_EFFECTS) {
														PotionEffect effect = target.getPotionEffect(effectType);
														if (effect != null) {
															debuffCount++;
														}
													}
													double damage = p.BASE_DAMAGE + p.DAMAGE_PER_DEBUFF * debuffCount;

													if (damage > 0) {
														BossUtils.blockableDamage(mBoss, target, p.DAMAGE_TYPE, damage, p.SPELL_NAME, mBoss.getLocation());
													}

													if (p.DAMAGE_PERCENTAGE > 0.0) {
														BossUtils.bossDamagePercent(mBoss, target, p.DAMAGE_PERCENTAGE, mBoss.getLocation(), p.SPELL_NAME);
													}
													p.EFFECTS.apply(target, mBoss);

													p.PARTICLE_HIT.spawn(boss, target.getLocation());
												}
											}
										}
									}
									p.SOUND_CAST.play(mBoss.getLocation());
									this.cancel();
								}
							}
						};
						runB.runTaskTimer(mPlugin, 0, 1);
						mActiveRunnables.add(runB);
					}

					@Override
					public int cooldownTicks() {
						return p.COOLDOWN;
					}

					@Override
					public boolean canRun() {
						return !p.TARGETS.getTargetsList(mBoss).isEmpty();
					}
				}
			));

		super.constructBoss(spell, Collections.emptyList(), (int) (p.TARGETS.getRange() * 2), null, p.DELAY);
	}
}
