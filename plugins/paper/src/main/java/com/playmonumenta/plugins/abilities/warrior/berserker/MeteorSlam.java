package com.playmonumenta.plugins.abilities.warrior.berserker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.berserker.MeteorSlamCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;


public final class MeteorSlam extends Ability {
	public static final String NAME = "Meteor Slam";
	public static final ClassAbility ABILITY = ClassAbility.METEOR_SLAM;
	private static final String SLAM_ONCE_THIS_TICK_METAKEY = "MeteorSlamTickSlammed";

	public static final double SCALING_DAMAGE_1 = 1;
	public static final double SCALING_DAMAGE_2 = 1.25;
	public static final double REDUCTION_MULTIPLIER = 0.1;
	public static final int DAMAGE_CAP_1 = 4;
	public static final int DAMAGE_CAP_2 = 5;
	public static final int SIZE_1 = 2;
	public static final int SIZE_2 = 3;
	public static final int JUMP_AMPLIFIER_1 = 3;
	public static final int JUMP_LEVEL_1 = JUMP_AMPLIFIER_1 + 1;
	public static final int JUMP_AMPLIFIER_2 = 4;
	public static final int JUMP_LEVEL_2 = JUMP_AMPLIFIER_2 + 1;
	public static final int DURATION_SECONDS = 2;
	public static final int DURATION_TICKS = DURATION_SECONDS * 20;
	public static final int AUTOMATIC_THRESHOLD = 3; // Minimum fall distance for landing to automatically trigger slam attack
	public static final int SCALING_THRESHOLD = 4; // Blocks fallen after which damage per block fallen increment does not increase
	public static final double REDUCED_THRESHOLD_1 = 5; // Fall distance past which damage transitions from starting to ending damage
	public static final double REDUCED_THRESHOLD_2 = 5.5;
	public static final int COOLDOWN_SECONDS_1 = 8;
	public static final int COOLDOWN_TICKS_1 = COOLDOWN_SECONDS_1 * 20;
	public static final int COOLDOWN_SECONDS_2 = 6;
	public static final int COOLDOWN_TICKS_2 = COOLDOWN_SECONDS_2 * 20;

	public static final String CHARM_DAMAGE = "Meteor Slam Damage";
	public static final String CHARM_RADIUS = "Meteor Slam Radius";
	public static final String CHARM_JUMP_BOOST = "Meteor Slam Jump Boost";
	public static final String CHARM_DURATION = "Meteor Slam Duration";
	public static final String CHARM_THRESHOLD = "Meteor Slam Fall Requirement";
	public static final String CHARM_COOLDOWN = "Meteor Slam Cooldown";

	public static final AbilityInfo<MeteorSlam> INFO =
		new AbilityInfo<>(MeteorSlam.class, NAME, MeteorSlam::new)
			.linkedSpell(ABILITY)
			.scoreboardId("MeteorSlam")
			.shorthandName("MS")
			.descriptions(
				String.format(
					"Pressing the swap key grants you Jump Boost %s for %ss. Cooldown: %ss. " +
						"Falling more than %s blocks passively generates a slam when you land, " +
						"dealing melee damage to all enemies in a %s block radius around you. " +
						"Falling increases the damage scaling per block fallen by +%s melee damage linearly, " +
						"starting at +%s damage for the first block fallen and capping at +%s melee damage per block fallen. " +
						"The damage scaling per block fallen is reduced by %s%% after the initial %s blocks. " +
						"If any enemies are damaged by a slam, you take no fall damage from that fall.",
					JUMP_LEVEL_1,
					DURATION_SECONDS,
					COOLDOWN_SECONDS_1,
					AUTOMATIC_THRESHOLD,
					SIZE_1,
					SCALING_DAMAGE_1,
					SCALING_DAMAGE_1,
					DAMAGE_CAP_1,
					StringUtils.multiplierToPercentage(1 - REDUCTION_MULTIPLIER),
					REDUCED_THRESHOLD_1
				),
				String.format(
					"Jump Boost level is increased from %s to %s. Cooldown is reduced from %ss to %ss. " +
						"Damage size is increased from %s to %s blocks. " +
						"Reduction threshold is increased from %s to %s blocks. " +
						"Damage scaling per block fallen is increased from +%s to +%s, " +
						"starting at +%s for the first block fallen and capping at +%s melee damage per block fallen instead. ",
					JUMP_LEVEL_1,
					JUMP_LEVEL_2,
					COOLDOWN_SECONDS_1,
					COOLDOWN_SECONDS_2,
					SIZE_1,
					SIZE_2,
					REDUCED_THRESHOLD_1,
					REDUCED_THRESHOLD_2,
					SCALING_DAMAGE_1,
					SCALING_DAMAGE_2,
					SCALING_DAMAGE_2,
					DAMAGE_CAP_2
				)
			)
			.simpleDescription("Gain jump boost, and deal area damage when you fall from heights.")
			.cooldown(COOLDOWN_TICKS_1, COOLDOWN_TICKS_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", MeteorSlam::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP)))
			.displayItem(Material.FIRE_CHARGE);

	private final double mLevelDamage;
	private final double mLevelDamageCap;
	private final double mLevelSize;
	private final int mLevelJumpAmplifier;
	private final double mReducedThreshold;
	private final BukkitRunnable mSlamAttackRunner;
	private double mFallFromY = -7050;
	private final MeteorSlamCS mCosmetic;

	public MeteorSlam(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mLevelDamage = isLevelOne() ? SCALING_DAMAGE_1 : SCALING_DAMAGE_2;
		mLevelDamageCap = isLevelOne() ? DAMAGE_CAP_1 : DAMAGE_CAP_2;
		mLevelSize = CharmManager.getRadius(mPlayer, CHARM_RADIUS, (isLevelOne() ? SIZE_1 : SIZE_2));
		mLevelJumpAmplifier = (isLevelOne() ? JUMP_AMPLIFIER_1 : JUMP_AMPLIFIER_2) + (int) CharmManager.getLevel(mPlayer, CHARM_JUMP_BOOST);
		mReducedThreshold = isLevelOne() ? REDUCED_THRESHOLD_1 : REDUCED_THRESHOLD_2;

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new MeteorSlamCS());

		mSlamAttackRunner = new BukkitRunnable() {
			@Override
			public void run() {
				if (player == null) {
					this.cancel();
					return;
				}
				if (
					AbilityManager.getManager().getPlayerAbility(player, MeteorSlam.class) == null
						|| !player.isValid() // Ensure player is not dead, is still online?
				) {
					// If reached this point but not silenced, then proceed with cancelling
					// If silenced, only return to not run anything, but don't cancel runnable
					if (!AbilityManager.getManager().getPlayerAbilities(player).isSilenced()) {
						this.cancel();
					}
					return;
				}

				if (!PlayerUtils.isOnGround(player)) {
					updateFallFrom(); // Vanilla fall distance would be 0 if on ground
				} else {
					// Currently on ground

					// If first tick landing, should still have old mFallFromY to calculate using
					// Therefore can damage if eligible
					if (calculateFallDistance() > (AUTOMATIC_THRESHOLD + CharmManager.getLevel(mPlayer, CHARM_THRESHOLD))) {
						// Only for checking in LivingEntityDamagedByPlayerEvent below,
						// so doesn't slam twice, since this doesn't yet set fall distance to 0
						MetadataUtils.checkOnceThisTick(plugin, player, SLAM_ONCE_THIS_TICK_METAKEY);

						doSlamAttack(player.getLocation().add(0, 0.15, 0));
					}

					// Whether or not did attack, now that on ground, forget mFallFromY
					mFallFromY = -7050;
				}
			}
		};
		cancelOnDeath(mSlamAttackRunner.runTaskTimer(plugin, 0, 1));
	}

	public void cast() {
		if (isOnCooldown()
			    || ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)) {
			return;
		}
		putOnCooldown();

		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION_TICKS), mLevelJumpAmplifier, true, false));

		World world = mPlayer.getWorld();
		Location location = mPlayer.getLocation().add(0, 0.15, 0);
		mCosmetic.slamCastEffect(world, location, mPlayer);
	}

	@Override
	public void invalidate() {
		if (mSlamAttackRunner != null) {
			mSlamAttackRunner.cancel();
		}
	}

	private void updateFallFrom() {
		// player.getFallDistance() is unreliable (e.g. does not get reset while in a bed, despite player.isOnGround() being true),
		// thus we calculate the fall distance ourselves.
		// We still check if fall distance is 0 to reset our fall distance calculation, as that checks for water, vines, etc.
		if (mPlayer.getFallDistance() <= 0) {
			mFallFromY = -10000;
		} else {
			mFallFromY = Math.max(mFallFromY, mPlayer.getLocation().getY());
		}
	}

	private double calculateFallDistance() {
		double currentY = mPlayer.getLocation().getY();
		double fallDistance = mFallFromY - currentY;
		return Math.max(fallDistance, 0);
	}

	private void doSlamAttack(Location location) {
		double fallDistance = calculateFallDistance();
		double linearFallDist = 0;
		double extraFallDist = 0;
		if (fallDistance > mReducedThreshold) {
			extraFallDist = fallDistance - mReducedThreshold;
			linearFallDist = mReducedThreshold - SCALING_THRESHOLD;
			fallDistance = SCALING_THRESHOLD;
		} else if (fallDistance > SCALING_THRESHOLD) {
			linearFallDist = fallDistance - SCALING_THRESHOLD;
			fallDistance = SCALING_THRESHOLD;
		}
		/* simplified total damage = ax^2 + bx + 0 until cap where both a and b = scaling / 2 for each respective level.
		 * afterwards linear increment for cap damage between reduced threshold and scaling threshold
		 * and reduced cap damage for remainder fall distance
		 */
		double slamDamage = (mLevelDamage / 2) * Math.pow(fallDistance, 2) + (mLevelDamage / 2) * fallDistance // quadratic scaling
			                    + linearFallDist * mLevelDamageCap // linear scaling
			                    + extraFallDist * mLevelDamageCap * REDUCTION_MULTIPLIER; // reduced scaling

		slamDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, slamDamage);

		for (LivingEntity enemy : new Hitbox.SphereHitbox(location, mLevelSize).getHitMobs()) {
			DamageUtils.damage(mPlayer, enemy, DamageType.MELEE_SKILL, slamDamage, mInfo.getLinkedSpell(), true);
		}

		World world = mPlayer.getWorld();
		mCosmetic.slamAttackEffect(world, location, mPlayer, mLevelSize, fallDistance);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		// If there is a mob in range, cancel the fall damage
		if (event.getType() == DamageType.FALL && !new Hitbox.SphereHitbox(mPlayer.getLocation(), mLevelSize).getHitMobs().isEmpty()) {
			event.setCancelled(true);
		}
	}
}
