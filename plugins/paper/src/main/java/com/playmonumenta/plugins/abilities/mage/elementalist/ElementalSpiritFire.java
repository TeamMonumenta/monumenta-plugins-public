package com.playmonumenta.plugins.abilities.mage.elementalist;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
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

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class ElementalSpiritFire extends Ability {
	public static final String NAME = "Elemental Spirits";

	public static final int DAMAGE_1 = 10;
	public static final int DAMAGE_2 = 15;
	public static final double BOW_MULTIPLIER_1 = 0.25;
	public static final double BOW_MULTIPLIER_2 = 0.4;
	public static final double HITBOX = 1.5;
	public static final int COOLDOWN_TICKS = 10 * Constants.TICKS_PER_SECOND;

	private final int mLevelDamage;
	private final double mLevelBowMultiplier;
	private final Set<LivingEntity> mEnemiesAffected = new HashSet<>();

	private @Nullable ElementalArrows mElementalArrows;
	private @Nullable BukkitTask mPlayerParticlesGenerator;
	private @Nullable BukkitTask mEnemiesAffectedProcessor;

	public ElementalSpiritFire(Plugin plugin, @Nullable Player player) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = ClassAbility.ELEMENTAL_SPIRIT_FIRE;

		mInfo.mScoreboardId = "ElementalSpirit";
		mInfo.mShorthandName = "ES";
		mInfo.mDescriptions.add(
			String.format(
				"Two spirits accompany you - one of fire and one of ice. The next moment after you deal fire damage, the fire spirit instantly dashes from you towards the farthest enemy that spell hit, dealing %s magic damage to all enemies in a %s-block cube around it along its path. The next moment after you deal ice damage, the ice spirit warps to the closest enemy that spell hit and induces an extreme local climate, dealing %s magic damage to all enemies in a %s-block cube around it every second for %ss. If the spell was %s, the fire spirit does an additional %s%% of the bow's original damage, and for the ice spirit, an additional %s%%. The spirits' damage ignores iframes. Independent cooldown: %ss.",
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

		mLevelDamage = isLevelOne() ? DAMAGE_1 : DAMAGE_2;
		mLevelBowMultiplier = isLevelOne() ? BOW_MULTIPLIER_1 : BOW_MULTIPLIER_2;

		// Task runs on the next server tick. Need to wait for entire AbilityCollection to be initialised to properly getPlayerAbility()
		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mElementalArrows = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(mPlayer, ElementalArrows.class);
			});
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		ClassAbility ability = event.getAbility();
		if (ability != null && (ability.equals(ClassAbility.ELEMENTAL_ARROWS_FIRE) || ability.equals(ClassAbility.STARFALL) || ability.equals(ClassAbility.MAGMA_SHIELD))) {
			mEnemiesAffected.add(event.getDamagee());
			// 1 runnable processes everything 1 tick later, so all enemies to affect are in
			if (mEnemiesAffectedProcessor == null) {
				mEnemiesAffectedProcessor = new BukkitRunnable() {
					@Override
					public void run() {
						mEnemiesAffectedProcessor = null;

						Location playerLocation = mPlayer.getLocation();
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

							Location startLocation = LocationUtils.getHalfHeightLocation(mPlayer);
							Location endLocation = LocationUtils.getHalfHeightLocation(farthestEnemy);

							World world = mPlayer.getWorld();
							BoundingBox movingSpiritBox = BoundingBox.of(mPlayer.getEyeLocation(), HITBOX, HITBOX, HITBOX);
							double maxDistanceSquared = startLocation.distanceSquared(endLocation);
							double maxDistance = Math.sqrt(maxDistanceSquared);
							Vector vector = endLocation.clone().subtract(startLocation).toVector();
							double increment = 0.2;

							List<LivingEntity> potentialTargets = EntityUtils.getNearbyMobs(playerLocation, maxDistance + HITBOX);
							float spellDamage = SpellPower.getSpellDamage(mPlugin, mPlayer, mLevelDamage);
							Vector vectorIncrement = vector.normalize().multiply(increment);

							// Fire spirit sound
							world.playSound(playerLocation, Sound.ENTITY_BLAZE_AMBIENT, 1, 0.5f);

							// Damage action & particles
							double maxIterations = maxDistance / increment * 1.1;
							for (int i = 0; i < maxIterations; i++) {
								Iterator<LivingEntity> iterator = potentialTargets.iterator();
								while (iterator.hasNext()) {
									LivingEntity potentialTarget = iterator.next();
									if (potentialTarget.getBoundingBox().overlaps(movingSpiritBox)) {
										double finalDamage = spellDamage;
										if (
											ClassAbility.ELEMENTAL_ARROWS_FIRE.equals(ability)
											&& mElementalArrows != null
										) {
											finalDamage += mElementalArrows.getLastDamage() * mLevelBowMultiplier;
										}

										DamageUtils.damage(mPlayer, potentialTarget, DamageType.MAGIC, finalDamage, mInfo.mLinkedSpell, true);
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
									PartialParticle partialParticle = new PartialParticle(Particle.FLAME, newPotentialLocation)
										.count(4)
										.delta(PartialParticle.getWidthDelta(HITBOX))
										.extra(0.05)
										.minimumMultiplier(false)
										.spawnAsPlayerActive(mPlayer);
									partialParticle
										.particle(Particle.SMOKE_LARGE)
										.spawnAsPlayerActive(mPlayer);
								}
							}
						}
					}
				}.runTaskLater(mPlugin, 2);
			}
		}
		return false;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		// Periodic trigger starts running again when the skill is off cooldown,
		// which restarts these passive particles
		if (mPlayerParticlesGenerator == null && mPlayer != null) {
			mPlayerParticlesGenerator = new BukkitRunnable() {
				double mVerticalAngle = 0;
				double mRotationAngle = 0;
				final PPPeriodic mParticle = new PPPeriodic(Particle.FLAME, mPlayer.getLocation()).extra(0.01);

				@Override
				public void run() {
					if (isTimerActive()
						    || mPlayer == null
							|| !mPlayer.isValid() // Ensure player is not dead, is still online?
						    || PremiumVanishIntegration.isInvisibleOrSpectator(mPlayer)) {
						this.cancel();
						mPlayerParticlesGenerator = null;
					}

					mVerticalAngle += 5.5;
					mRotationAngle += 10;
					mVerticalAngle %= 360;
					mRotationAngle %= 360;

					mParticle.location(
						LocationUtils
							.getHalfHeightLocation(mPlayer)
							.add(
								FastUtils.cos(Math.toRadians(mRotationAngle)),
								FastUtils.sin(Math.toRadians(mVerticalAngle)) * 0.5,
								FastUtils.sin(Math.toRadians(mRotationAngle))
								))
						.spawnAsPlayerPassive(mPlayer);
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
