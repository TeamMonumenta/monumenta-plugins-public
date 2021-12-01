package com.playmonumenta.plugins.abilities.mage.elementalist;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.abilities.SpellPower;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.StringUtils;



public class ElementalSpiritFire extends Ability {
	@NotNull public static final String NAME = "Elemental Spirits";
	@NotNull public static final ClassAbility ABILITY = ClassAbility.ELEMENTAL_SPIRIT_FIRE;

	public static final int DAMAGE_1 = 10;
	public static final int DAMAGE_2 = 15;
	public static final double BOW_MULTIPLIER_1 = 0.25;
	public static final double BOW_MULTIPLIER_2 = 0.4;
	public static final double HITBOX = 1.5;
	public static final int COOLDOWN_TICKS = 10 * Constants.TICKS_PER_SECOND;

	private final int mLevelDamage;
	private final double mLevelBowMultiplier;
	private final @NotNull Set<LivingEntity> mEnemiesAffected = new HashSet<>();

	private @Nullable ElementalArrows mElementalArrows;
	private @Nullable BukkitTask mPlayerParticlesGenerator;
	private @Nullable BukkitTask mEnemiesAffectedProcessor;

	public ElementalSpiritFire(Plugin plugin, Player player) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = ABILITY;

		mInfo.mScoreboardId = "ElementalSpirit";
		mInfo.mShorthandName = "ES";
		mInfo.mDescriptions.add(
			String.format(
				"Two spirits accompany you - one of fire and one of ice. The next moment after you deal fire damage, the fire spirit instantly dashes from you towards the farthest enemy that spell hit, dealing %s fire damage to all enemies in a %s-block cube around it along its path. The next moment after you deal ice damage, the ice spirit warps to the closest enemy that spell hit and induces an extreme local climate, dealing %s ice damage to all enemies in a %s-block cube around it every second for %ss. If the spell was %s, the fire spirit does an additional %s%% of the bow's original damage, and for the ice spirit, an additional %s%%. The spirits' damage ignores iframes. Independent cooldown: %ss.",
				DAMAGE_1,
				HITBOX,
				ElementalSpiritIce.DAMAGE_1,
				ElementalSpiritIce.SIZE,
				ElementalSpiritIce.PULSES,
				ElementalArrows.NAME,
				StringUtils.multiplierToPercentage(BOW_MULTIPLIER_1),
				StringUtils.multiplierToPercentage(ElementalSpiritIce.BOW_MULTIPLIER_1),
				StringUtils.ticksToSeconds(COOLDOWN_TICKS)
			) // Ice pulse interval of 20 ticks hardcoded to say "every second"
		);
		mInfo.mDescriptions.add(
			String.format(
				"Fire spirit damage is increased from %s to %s. Ice spirit damage is increased from %s to %s. Bonus %s damage is increased from %s%% to %s%% for the fire spirit, and from %s%% to %s%% for the ice spirit.",
				DAMAGE_1,
				DAMAGE_2,
				ElementalSpiritIce.DAMAGE_1,
				ElementalSpiritIce.DAMAGE_2,
				ElementalArrows.NAME,
				StringUtils.multiplierToPercentage(BOW_MULTIPLIER_1),
				StringUtils.multiplierToPercentage(BOW_MULTIPLIER_2),
				StringUtils.multiplierToPercentage(ElementalSpiritIce.BOW_MULTIPLIER_1),
				StringUtils.multiplierToPercentage(ElementalSpiritIce.BOW_MULTIPLIER_2)
			)
		);
		mInfo.mCooldown = COOLDOWN_TICKS;
		mDisplayItem = new ItemStack(Material.SUNFLOWER, 1);

		boolean isUpgraded = getAbilityScore() == 2;
		mLevelDamage = isUpgraded ? DAMAGE_2 : DAMAGE_1;
		mLevelBowMultiplier = isUpgraded ? BOW_MULTIPLIER_2 : BOW_MULTIPLIER_1;

		// Task runs on the next server tick. Need to wait for entire AbilityCollection to be initialised to properly getPlayerAbility()
		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mElementalArrows = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(mPlayer, ElementalArrows.class);
			});
		}
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {
		if (
			MagicType.FIRE.equals(event.getMagicType())
			&& !ABILITY.equals(event.getSpell())
		) {
			mEnemiesAffected.add(event.getDamaged());
			// 1 runnable processes everything 1 tick later, so all enemies to affect are in
			if (mEnemiesAffectedProcessor == null) {
				mEnemiesAffectedProcessor = new BukkitRunnable() {
					@Override
					public void run() {
						mEnemiesAffectedProcessor = null;

						@NotNull Location playerLocation = mPlayer.getLocation();
						@Nullable LivingEntity farthestEnemy = null;
						double farthestDistanceSquared = 0;

						for (LivingEntity enemy : mEnemiesAffected) {
							if (enemy.isValid()) { // If neither dead nor despawned
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

							@NotNull Location startLocation = LocationUtils.getHalfHeightLocation(mPlayer);
							@NotNull Location endLocation = LocationUtils.getHalfHeightLocation(farthestEnemy);

							@NotNull World world = mPlayer.getWorld();
							@NotNull BoundingBox movingSpiritBox = BoundingBox.of(mPlayer.getEyeLocation(), HITBOX, HITBOX, HITBOX);
							double maxDistanceSquared = startLocation.distanceSquared(endLocation);
							double maxDistance = Math.sqrt(maxDistanceSquared);
							@NotNull Vector vector = endLocation.clone().subtract(startLocation).toVector();
							double increment = 0.2;

							@NotNull List<LivingEntity> potentialTargets = EntityUtils.getNearbyMobs(playerLocation, maxDistance + HITBOX);
							float spellDamage = SpellPower.getSpellDamage(mPlayer, mLevelDamage);
							@NotNull Vector vectorIncrement = vector.normalize().multiply(increment);

							// Fire spirit sound
							world.playSound(playerLocation, Sound.ENTITY_BLAZE_AMBIENT, 1, 0.5f);

							// Damage action & particles
							double maxIterations = maxDistance / increment * 1.1;
							for (int i = 0; i < maxIterations; i++) {
								@NotNull Iterator<LivingEntity> iterator = potentialTargets.iterator();
								while (iterator.hasNext()) {
									@NotNull LivingEntity potentialTarget = iterator.next();
									if (potentialTarget.getBoundingBox().overlaps(movingSpiritBox)) {
										float finalDamage = spellDamage;
										if (
											ClassAbility.ELEMENTAL_ARROWS.equals(event.getSpell())
											&& mElementalArrows != null
										) {
											finalDamage += mElementalArrows.getLastDamage() * mLevelBowMultiplier;
										}

										EntityUtils.damageEntity(mPlugin, potentialTarget, finalDamage, mPlayer, MagicType.FIRE, true, ABILITY, true, true, true);
										iterator.remove();
									}
								}

								// The first shift happens after the first damage attempt,
								// unlike something like LocationUtils.travelTillObstructed().
								// The spirit starts at the player's eyes so this could damage enemies right beside/behind them
								movingSpiritBox.shift(vectorIncrement);
								Location newPotentialLocation = movingSpiritBox.getCenter().toLocation(world);
								if (playerLocation.distanceSquared(newPotentialLocation) > maxDistanceSquared) {
									break;
								} else {
									// Else spawn particles at the new location and continue doing damage at this place the next tick
									// These particles skip the first damage attempt
									@NotNull PartialParticle partialParticle = new PartialParticle(
										Particle.FLAME,
										newPotentialLocation,
										4,
										PartialParticle.getWidthDelta(HITBOX),
										0.05
									).spawnAsPlayer(mPlayer);
									partialParticle.mParticle = Particle.SMOKE_LARGE;
									partialParticle.spawnAsPlayer(mPlayer);
								}
							}
						}
					}
				}.runTask(mPlugin);
			}
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		// Periodic trigger starts running again when the skill is off cooldown,
		// which restarts these passive particles
		if (mPlayerParticlesGenerator == null) {
			mPlayerParticlesGenerator = new BukkitRunnable() {
				double mVerticalAngle = 0;
				double mRotationAngle = 0;

				@Override
				public void run() {
					if (
						isTimerActive()
						|| !mPlayer.isValid() // Ensure player is not dead, is still online?
						|| PremiumVanishIntegration.isInvisible(mPlayer)
					) {
						this.cancel();
						mPlayerParticlesGenerator = null;
					}

					mVerticalAngle += 5.5;
					mRotationAngle += 10;
					mVerticalAngle %= 360;
					mRotationAngle %= 360;

					new PartialParticle(
						Particle.FLAME,
						LocationUtils
							.getHalfHeightLocation(mPlayer)
							.add(
								FastUtils.cos(Math.toRadians(mRotationAngle)),
								FastUtils.sin(Math.toRadians(mVerticalAngle)) * 0.5,
								FastUtils.sin(Math.toRadians(mRotationAngle))
							),
						1,
						0,
						0.01
					).spawnAsPlayer(mPlayer, true);
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public void invalidate() {
		if (mPlayerParticlesGenerator != null) {
			mPlayerParticlesGenerator.cancel();
		}

		if (mEnemiesAffectedProcessor != null) {
			mEnemiesAffectedProcessor.cancel();
		}
	}
}
