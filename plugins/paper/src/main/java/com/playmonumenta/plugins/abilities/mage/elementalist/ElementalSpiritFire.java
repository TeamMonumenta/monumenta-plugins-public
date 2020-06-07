package com.playmonumenta.plugins.abilities.mage.elementalist;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;


public class ElementalSpiritFire extends Ability {

	private static final int ES_FIRE_COOLDOWN = 20 * 12;
	private static final int ES_FIRE_1_DAMAGE = 12;
	private static final int ES_FIRE_2_DAMAGE = 21;
	private static final double ES_FIRE_SIZE = 1.5;

	private final int mDamage;
	private final Set<LivingEntity> mMobsDamaged = new HashSet<>();
	private BukkitRunnable mMobsDamagedParser;
	private BukkitRunnable mParticleGenerator;

	public ElementalSpiritFire(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Elemental Spirit");
		mInfo.mScoreboardId = "ElementalSpirit";
		mInfo.mShorthandName = "ES";
		mInfo.mDescriptions.add("You are accompanied a spirit of fire and a spirit of ice. Upon using a fire spell, the fire spirit will rush towards the farthest enemy hit with the spell, damaging all enemies along the way by 12. Upon using an ice spell, the ice spirit will rush towards the closest enemy hit with the spell, damaging mobs in a 3 block radius by 4 per second for 3 seconds. Each spirit operates on its own cooldown of 12s.");
		mInfo.mDescriptions.add("Damage dealt by the fire spirit is increased to 21, and damage dealt by the ice spirit is increased to 7.");
		mInfo.mLinkedSpell = Spells.ELEMENTAL_SPIRIT_FIRE;
		mInfo.mCooldown = ES_FIRE_COOLDOWN;
		mDamage = getAbilityScore() == 1 ? ES_FIRE_1_DAMAGE : ES_FIRE_2_DAMAGE;
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {
		if (event.getMagicType() == MagicType.FIRE && event.getSpell() != null && !event.getSpell().equals(mInfo.mLinkedSpell)) {
			mMobsDamaged.add(event.getDamaged());

			// We make 1 runnable that processes everything 1 tick later, so all the mob information is in.
			if (mMobsDamagedParser == null) {
				mMobsDamagedParser = new BukkitRunnable() {
					@Override
					public void run() {
						Location loc = mPlayer.getLocation();
						LivingEntity farthestMob = null;
						double farthestDistance = -1;

						for (LivingEntity mob : mMobsDamaged) {
							if (mob.isValid() && !mob.isDead()) {
								double distance = loc.distanceSquared(mob.getLocation());
								if (distance > farthestDistance) {
									farthestDistance = distance;
									farthestMob = mob;
								}
							}
						}

						if (farthestMob != null) {
							farthestDistance = Math.sqrt(farthestDistance);
							List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, farthestDistance + 1);
							Vector dir = farthestMob.getLocation().subtract(loc).toVector().normalize();
							BoundingBox fireSpirit = BoundingBox.of(mPlayer.getEyeLocation(), ES_FIRE_SIZE, ES_FIRE_SIZE, ES_FIRE_SIZE);

							mWorld.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 1, 0.5f);

							for (int i = 0; i < (int)(farthestDistance + 1); i++) {
								Iterator<LivingEntity> iter = mobs.iterator();
								while (iter.hasNext()) {
									LivingEntity mob = iter.next();
									if (mob.getBoundingBox().overlaps(fireSpirit)) {
										mob.setNoDamageTicks(0);
										EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.FIRE, true, mInfo.mLinkedSpell);
										iter.remove();
									}
								}

								fireSpirit.shift(dir);
								mWorld.spawnParticle(Particle.FLAME, fireSpirit.getCenterX(), fireSpirit.getCenterY(), fireSpirit.getCenterZ(), 20, 0.4, 0.4, 0.4, 0.01);
								mWorld.spawnParticle(Particle.SMOKE_LARGE, fireSpirit.getCenterX(), fireSpirit.getCenterY(), fireSpirit.getCenterZ(), 10, 0.4, 0.4, 0.4, 0.01);
							}

							putOnCooldown();
						}

						this.cancel();
						mMobsDamagedParser = null;
						mMobsDamaged.clear();
					}
				};

				mMobsDamagedParser.runTaskLater(mPlugin, 1);
			}
		}
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (mParticleGenerator == null) {
			mParticleGenerator = new BukkitRunnable() {
				float mVertAngle = 0f;
				double mRotation = 0;

				@Override
				public void run() {
					Location loc = mPlayer.getLocation().add(0, 1, 0);
					mVertAngle += 0.1f;
					mRotation += 10;
					double radian1 = Math.toRadians(mRotation);
					loc.add(Math.cos(radian1), Math.sin(mVertAngle) * 0.5, Math.sin(radian1));

					// Don't display particles to player if they're in their face
					if (loc.clone().subtract(mPlayer.getLocation().add(0, 1, 0)).toVector().normalize().dot(mPlayer.getEyeLocation().getDirection()) > 0.25) {
						for (Player other : PlayerUtils.playersInRange(mPlayer, 30, false)) {
							other.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0.01);
						}
					} else {
						mWorld.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0.01);
					}

					if (AbilityManager.getManager().getPlayerAbility(mPlayer, ElementalSpiritFire.class) == null
					    || !mPlayer.isOnline()
					    || mPlayer.isDead()
					    || mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), Spells.ELEMENTAL_SPIRIT_FIRE)) {
						this.cancel();
						mParticleGenerator = null;
					}

					loc.subtract(Math.cos(radian1), Math.sin(mVertAngle) * 0.5, Math.sin(radian1));
				}

			};

			mParticleGenerator.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public void invalidate() {
		if (mParticleGenerator != null) {
			mParticleGenerator.cancel();
		}
	}
}
