package com.playmonumenta.plugins.abilities.mage.elementalist;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;


public class ElementalSpiritIce extends Ability {

	private static final int ES_ICE_COOLDOWN = 20 * 12;
	private static final int ES_ICE_1_DAMAGE = 4;
	private static final int ES_ICE_2_DAMAGE = 7;
	private static final int ES_ICE_RADIUS = 3;
	private static final int ES_ICE_PULSES = 3;
	private static final int ES_ICE_PULSE_INTERVAL = 20 * 1;

	private final int mDamage;
	private final Set<LivingEntity> mMobsDamaged = new HashSet<LivingEntity>();
	private BukkitRunnable mMobsDamagedParser;
	private BukkitRunnable mParticleGenerator;

	public ElementalSpiritIce(Plugin plugin, World world, Player player) {
		/* NOTE: Display name is null so this variant will be ignored by the tesseract.
		 * This variant also does not have a description */
		super(plugin, world, player, null);
		mInfo.scoreboardId = "ElementalSpirit";
		mInfo.linkedSpell = Spells.ELEMENTAL_SPIRIT_ICE;
		mInfo.cooldown = ES_ICE_COOLDOWN;
		mDamage = getAbilityScore() == 1 ? ES_ICE_1_DAMAGE : ES_ICE_2_DAMAGE;
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {
		if (event.getMagicType() == MagicType.ICE && event.getSpell() != null && !event.getSpell().equals(mInfo.linkedSpell)) {
			mMobsDamaged.add(event.getDamaged());

			// We make 1 runnable that processes everything 1 tick later, so all the mob information is in.
			if (mMobsDamagedParser == null) {
				mMobsDamagedParser = new BukkitRunnable() {
					@Override
					public void run() {
						LivingEntity closestMob = null;
						double closestDistance = 9001;

						for (LivingEntity mob : mMobsDamaged) {
							if (mob.isValid() && !mob.isDead()) {
								double distance = mPlayer.getLocation().distanceSquared(mob.getLocation());
								if (distance < closestDistance) {
									closestDistance = distance;
									closestMob = mob;
								}
							}
						}

						if (closestMob != null) {
							Location loc = closestMob.getLocation();

							new BukkitRunnable() {
								Location mLoc = loc.add(0, 1, 0);
								int mPulses = 0;

								@Override
								public void run() {
									mWorld.spawnParticle(Particle.SNOWBALL, mLoc, 150, 2, 0.25, 2, 0.1);
									mWorld.spawnParticle(Particle.FIREWORKS_SPARK, mLoc, 30, 2, 0.25, 2, 0.1);
									mWorld.playSound(mLoc, Sound.ENTITY_TURTLE_HURT_BABY, 1, 0.2f);
									mWorld.playSound(mLoc, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.05f);
									for (LivingEntity mob : EntityUtils.getNearbyMobs(mLoc, ES_ICE_RADIUS)) {
										mob.setNoDamageTicks(0);
										EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.ICE, true, mInfo.linkedSpell);
										mob.setVelocity(new Vector(0, 0, 0));
									}

									mPulses++;
									if (mPulses >= ES_ICE_PULSES) {
										this.cancel();
									}
								}
							}.runTaskTimer(mPlugin, ES_ICE_PULSE_INTERVAL, ES_ICE_PULSE_INTERVAL);

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
					mVertAngle -= 0.1f;
					mRotation += 10;
					double radian1 = Math.toRadians(mRotation);
					loc.add(Math.cos(radian1), Math.sin(mVertAngle) * 0.5, Math.sin(radian1));

					// Don't display particles to player if they're in their face
					if (loc.clone().subtract(mPlayer.getLocation().add(0, 1, 0)).toVector().normalize().dot(mPlayer.getEyeLocation().getDirection()) > 0.25) {
						for (Player other : PlayerUtils.playersInRange(mPlayer, 30, false)) {
							other.spawnParticle(Particle.SNOWBALL, loc, 3, 0, 0, 0, 0);
						}
					} else {
						mWorld.spawnParticle(Particle.SNOWBALL, loc, 3, 0, 0, 0, 0);
					}

					if (AbilityManager.getManager().getPlayerAbility(mPlayer, ElementalSpiritIce.class) == null ||
						!mPlayer.isOnline() || mPlayer == null || mPlayer.isDead() ||
						mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), Spells.ELEMENTAL_SPIRIT_ICE)) {
						this.cancel();
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
