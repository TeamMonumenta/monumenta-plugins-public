package com.playmonumenta.plugins.abilities.mage.elementalist;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;



public class ElementalSpiritFire extends Ability {
	public static final String NAME = "Elemental Spirits";

	public static final int DAMAGE_1 = 10;
	public static final int DAMAGE_2 = 15;
	public static final double SIZE = 1.5;
	public static final double BOW_MULTIPLIER_1 = 0.25;
	public static final double BOW_MULTIPLIER_2 = 0.4;
	public static final int BOW_PERCENTAGE_1 = (int) (BOW_MULTIPLIER_1 * 100);
	public static final int BOW_PERCENTAGE_2 = (int) (BOW_MULTIPLIER_2 * 100);
	public static final int COOLDOWN_SECONDS = 10;
	public static final int COOLDOWN = COOLDOWN_SECONDS * 20;

	private final Set<LivingEntity> mEnemiesAffected = new HashSet<>();
	private final int mLevelDamage;
	private final double mElementalArrowsBowDamage;

	private ElementalArrows mElementalArrows;
	private BukkitRunnable mEnemiesAffectedProcessor;
	private BukkitRunnable mPlayerParticlesGenerator;

	public ElementalSpiritFire(Plugin plugin, Player player) {
		super(plugin, player, NAME);

		mInfo.mLinkedSpell = Spells.ELEMENTAL_SPIRIT_FIRE;
		mInfo.mScoreboardId = "ElementalSpirit";
		mInfo.mShorthandName = "ES";
		//TODO update, is 21 tick wait for ice spirit intended? Description does not mention it
		mInfo.mDescriptions.add(
			String.format(
				"Two spirits accompany you - one of fire and one of ice. The next moment after you cast a fire spell, the fire spirit instantly dashes from you towards the farthest enemy that spell hit, dealing %s damage to all enemies in a %s-block cube around it along its path. The next moment after you cast an ice spell, the ice spirit warps to the closest enemy that spell hit, dealing %s damage to all enemies in a %s-block cube around it every second for %s seconds. If the spell was %s, the fire spirit does an additional %s%% of the bow's original damage, and for the ice spirit, an additional %s%%. Each spirit has an independent cooldown, and their damage ignores iframes. Cooldown: %ss.",
				DAMAGE_1,
				SIZE,
				ElementalSpiritIce.DAMAGE_1,
				ElementalSpiritIce.SIZE,
				ElementalSpiritIce.PULSES,
				ElementalArrows.NAME,
				BOW_PERCENTAGE_1,
				ElementalSpiritIce.BOW_PERCENTAGE_1,
				COOLDOWN_SECONDS
			) // Ice pulse interval of 20 ticks hardcoded to say "every second"
		);
		mInfo.mDescriptions.add(
			String.format(
				"Fire spirit damage is increased from %s to %s. Ice spirit damage is increased from %s to %s. The damage bonus from %s is increased to %s%% for the fire spirit, and %s%% for the ice spirit.",
				DAMAGE_1,
				DAMAGE_2,
				ElementalSpiritIce.DAMAGE_1,
				ElementalSpiritIce.DAMAGE_2,
				ElementalArrows.NAME,
				BOW_PERCENTAGE_2,
				ElementalSpiritIce.BOW_PERCENTAGE_2
			)
		);
		mInfo.mCooldown = COOLDOWN;

		mLevelDamage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
		mElementalArrowsBowDamage = getAbilityScore() == 1 ? BOW_MULTIPLIER_1 : BOW_MULTIPLIER_2;
		// Task runs on the next server tick. Need to wait for entire AbilityCollection to be initialised to properly getPlayerAbility()
		Bukkit.getScheduler().runTask(plugin, () -> {
			if (player != null) {
				mElementalArrows = AbilityManager.getManager().getPlayerAbility(mPlayer, ElementalArrows.class);
			}
		});
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {
		if (event.getMagicType() == MagicType.FIRE && event.getSpell() != null && !event.getSpell().equals(mInfo.mLinkedSpell)) {
			mEnemiesAffected.add(event.getDamaged());
			// 1 runnable processes everything 1 tick later, so all enemies to affect are in
			if (mEnemiesAffectedProcessor == null) {
				mEnemiesAffectedProcessor = new BukkitRunnable() {
					@Override
					public void run() {
						mEnemiesAffectedProcessor = null;

						Location playerLocation = mPlayer.getLocation();
						LivingEntity farthestEnemy = null;
						double farthestDistanceSquared = 0;

						for (LivingEntity enemy : mEnemiesAffected) {
							if (enemy.isValid()) { // If not dead or has despawned
								double distanceSquared = playerLocation.distanceSquared(enemy.getLocation());
								if (distanceSquared > farthestDistanceSquared) {
									farthestEnemy = enemy;
									farthestDistanceSquared = distanceSquared;
								}
							}
						}
						mEnemiesAffected.clear();

						if (farthestEnemy != null) {
							putOnCooldown();

							Location startLocation = mPlayer.getLocation().add(0, mPlayer.getHeight() / 2, 0);
							Location endLocation = farthestEnemy.getLocation().add(0, farthestEnemy.getHeight() / 2, 0);

							World world = mPlayer.getWorld();
							BoundingBox movingSpiritBox = BoundingBox.of(mPlayer.getEyeLocation(), SIZE, SIZE, SIZE);
							double maxDistance = startLocation.distance(endLocation);
							Vector vector = endLocation.clone().subtract(startLocation).toVector();
							double increment = 0.2;

							world.playSound(playerLocation, Sound.ENTITY_BLAZE_AMBIENT, 1, 0.5f);

							List<LivingEntity> potentialTargets = EntityUtils.getNearbyMobs(playerLocation, maxDistance + 1);
							float spellDamage = SpellDamage.getSpellDamage(mPlayer, mLevelDamage);
							Vector vectorIncrement = vector.normalize().multiply(increment);

							double maxIterations = maxDistance / increment * 1.1;
							for (int i = 0; i < maxIterations; i++) {
								Iterator<LivingEntity> iterator = potentialTargets.iterator();
								while (iterator.hasNext()) {
									LivingEntity potentialTarget = iterator.next();
									if (potentialTarget.getBoundingBox().overlaps(movingSpiritBox)) {
										float finalDamage = spellDamage;
										if (event.getSpell().equals(Spells.ELEMENTAL_ARROWS) && mElementalArrows != null) {
											
											finalDamage += mElementalArrows.getLastDamage() * mElementalArrowsBowDamage;
										}

										//TODO true damage bypass instead of iframe reset - https://discord.com/channels/186225508562763776/186225918086217729/816701492000981014
										potentialTarget.setNoDamageTicks(0);
										EntityUtils.damageEntity(mPlugin, potentialTarget, finalDamage, mPlayer, MagicType.FIRE, true, mInfo.mLinkedSpell);
										iterator.remove();
									}
								}

								// The first shift happens after the first damage attempt,
								// unlike something like LocationUtils.travelTillObstructed().
								// The spirit starts at the player's eyes so this could damage enemies right beside/behind the player
								movingSpiritBox.shift(vectorIncrement);
								Location newPotentialLocation = movingSpiritBox.getCenter().toLocation(world);
								if (playerLocation.distanceSquared(newPotentialLocation) > maxDistance * maxDistance) {
									break;
								} else {
									// Else spawn particles at the new location and continue doing damage at this place the next tick
									// These particles skip the first damage attempt
									world.spawnParticle(Particle.FLAME, newPotentialLocation, 4, 0.4, 0.4, 0.4, 0.1);
									world.spawnParticle(Particle.SMOKE_LARGE, newPotentialLocation, 4, 0.4, 0.4, 0.4, 0.1);
								}
							}
						}
					}
				};
				mEnemiesAffectedProcessor.runTaskLater(mPlugin, 1);
			}
		}
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (mPlayerParticlesGenerator == null) {
			mPlayerParticlesGenerator = new BukkitRunnable() {
				double mVerticalAngle = 0;
				double mRotationAngle = 0;

				@Override
				public void run() {
					mVerticalAngle += 5.5;
					mRotationAngle += 10;
					mVerticalAngle %= 360;
					mRotationAngle %= 360;

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
							otherPlayer.spawnParticle(Particle.FLAME, particleLocation, 1, 0, 0, 0, 0.01);
						}
					} else {
						mPlayer.getWorld().spawnParticle(Particle.FLAME, particleLocation, 1, 0, 0, 0, 0.01);
					}

					if (
						mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), Spells.ELEMENTAL_SPIRIT_FIRE)
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
