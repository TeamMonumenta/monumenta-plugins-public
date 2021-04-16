package com.playmonumenta.plugins.abilities.mage.elementalist;

import java.util.HashSet;
import java.util.Set;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.SpellDamage;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;



public class ElementalSpiritIce extends Ability {
	public static final Spells SPELL = Spells.ELEMENTAL_SPIRIT_ICE;

	public static final int DAMAGE_1 = 4;
	public static final int DAMAGE_2 = 6;
	public static final int SIZE = 3;
	public static final double BOW_MULTIPLIER_1 = 0.1;
	public static final int BOW_PERCENTAGE_1 = (int)(BOW_MULTIPLIER_1 * 100);
	public static final double BOW_MULTIPLIER_2 = 0.15;
	public static final int BOW_PERCENTAGE_2 = (int)(BOW_MULTIPLIER_2 * 100);
	public static final int PULSE_INTERVAL = 20;
	public static final int PULSES = 3;
	public static final int COOLDOWN_TICKS = ElementalSpiritFire.COOLDOWN_TICKS;

	private final int mLevelDamage;
	private final double mLevelBowMultiplier;
	private final Set<LivingEntity> mEnemiesAffected = new HashSet<>();

	private ElementalArrows mElementalArrows;
	private BukkitRunnable mEnemiesAffectedProcessor;
	private BukkitRunnable mPlayerParticlesGenerator;

	public ElementalSpiritIce(Plugin plugin, Player player) {
		/* NOTE
		 * Display name is null so this variant will be ignored by the tesseract.
		 * This variant also does not have a description
		 */
		super(plugin, player, null);
		mInfo.mLinkedSpell = SPELL;

		mInfo.mScoreboardId = "ElementalSpirit";
		mInfo.mCooldown = COOLDOWN_TICKS;

		boolean isUpgraded = getAbilityScore() == 2;
		mLevelDamage = isUpgraded ? DAMAGE_2 : DAMAGE_1;
		mLevelBowMultiplier = isUpgraded ? BOW_MULTIPLIER_2 : BOW_MULTIPLIER_1;

		Bukkit.getScheduler().runTask(plugin, () -> {
			if (player != null) {
				mElementalArrows = AbilityManager.getManager().getPlayerAbility(mPlayer, ElementalArrows.class);
			}
		});
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {
		if (event.getMagicType() == MagicType.ICE && event.getSpell() != null && !event.getSpell().equals(mInfo.mLinkedSpell)) {
			mEnemiesAffected.add(event.getDamaged());
			if (mEnemiesAffectedProcessor == null) {
				mEnemiesAffectedProcessor = new BukkitRunnable() {
					@Override
					public void run() {
						mEnemiesAffectedProcessor = null;

						Location playerLocation = mPlayer.getLocation();
						LivingEntity closestEnemy = null;
						double closestDistanceSquared = 7050;

						for (LivingEntity enemy : mEnemiesAffected) {
							if (enemy.isValid()) {
								double distanceSquared = playerLocation.distanceSquared(enemy.getLocation());
								if (distanceSquared < closestDistanceSquared) {
									closestDistanceSquared = distanceSquared;
									closestEnemy = enemy;
								}
							}
						}
						mEnemiesAffected.clear();

						if (closestEnemy != null) {
							putOnCooldown();

							Location damageCentre = closestEnemy.getLocation();
							Location particleCentre = damageCentre.clone().add(0, closestEnemy.getHeight() / 2, 0);
							float spellDamage = SpellDamage.getSpellDamage(mPlayer, mLevelDamage);
							new BukkitRunnable() {
								int mPulses = 1; // The current pulse for this run

								@Override
								public void run() {
									World world = mPlayer.getWorld();
									world.spawnParticle(Particle.SNOWBALL, particleCentre, 150, SIZE / 2, 0.25, SIZE / 2, 0.1);
									world.spawnParticle(Particle.FIREWORKS_SPARK, particleCentre, 30, SIZE / 2, 0.25, SIZE / 2, 0.1);
									world.playSound(particleCentre, Sound.ENTITY_TURTLE_HURT_BABY, 1, 0.2f);
									world.playSound(particleCentre, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.05f);

									for (LivingEntity mob : EntityUtils.getNearbyMobs(damageCentre, SIZE)) {
										float finalDamage = spellDamage;
										if (event.getSpell().equals(Spells.ELEMENTAL_ARROWS) && mElementalArrows != null) {
											finalDamage += mElementalArrows.getLastDamage() * mLevelBowMultiplier;
										}

										EntityUtils.damageEntity(mPlugin, mob, finalDamage, mPlayer, MagicType.ICE, true, mInfo.mLinkedSpell, true, true, true);
										mob.setVelocity(new Vector());
									}

									if (mPulses >= PULSES) {
										this.cancel();
									} else {
										mPulses++;
									}
								}
							}.runTaskTimer(mPlugin, 0, PULSE_INTERVAL);
						}
					}
				};
				mEnemiesAffectedProcessor.runTaskLater(mPlugin, 1);
			}
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mPlayerParticlesGenerator == null) {
			mPlayerParticlesGenerator = new BukkitRunnable() {
				double mVerticalAngle = 0;
				double mRotationAngle = 180;

				@Override
				public void run() {
					mVerticalAngle -= 5.5;
					mRotationAngle -= 10;
					mVerticalAngle %= -360;
					mRotationAngle %= -360;

					Location particleLocation = mPlayer.getLocation().add(0, mPlayer.getHeight() / 2, 0);
					particleLocation.add(
						FastUtils.cos(Math.toRadians(mRotationAngle)),
						FastUtils.sin(Math.toRadians(mVerticalAngle)) * 0.5,
						FastUtils.sin(Math.toRadians(mRotationAngle))
					);

					Location eyeLocation = mPlayer.getEyeLocation();
					Vector particleVector = particleLocation.clone().subtract(eyeLocation).toVector();
					Vector lookVector = eyeLocation.getDirection();
					if (particleVector.dot(lookVector) > 0.25) {
						// Don't display particles to player if they're in their face
						for (Player otherPlayer : PlayerUtils.playersInRange(mPlayer, 30, false)) {
							otherPlayer.spawnParticle(Particle.SNOWBALL, particleLocation, 3, 0, 0, 0, 0);
						}
					} else {
						mPlayer.getWorld().spawnParticle(Particle.SNOWBALL, particleLocation, 3, 0, 0, 0, 0);
					}

					if (
						isTimerActive(ElementalSpiritFire.SPELL)
						|| AbilityManager.getManager().getPlayerAbility(mPlayer, ElementalSpiritFire.class) == null
						|| !mPlayer.isValid() // Ensure player is not dead, is still online?
					) {
						this.cancel();
						mPlayerParticlesGenerator = null;
					}
				}
			};
			mPlayerParticlesGenerator.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public void invalidate() {
		if (mPlayerParticlesGenerator != null) {
			mPlayerParticlesGenerator.cancel();
		}
	}
}
