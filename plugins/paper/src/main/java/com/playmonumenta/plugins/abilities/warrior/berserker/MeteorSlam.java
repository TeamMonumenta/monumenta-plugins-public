package com.playmonumenta.plugins.abilities.warrior.berserker;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.berserker.MeteorSlamCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class MeteorSlam extends Ability {
	private static final String SLAM_ONCE_THIS_TICK_METAKEY = "MeteorSlamTickSlammed";
	private static final int SNEAK_TIME_REQ = 3;
	private static final int CAST_DELAY = 5;

	// Swing
	private static final double VAULT_VELOCITY = 1.1;
	private static final double VAULT_VELOCITY_PENALTY = 0.5;
	private static final double UP_DAMAGE = 10;
	private static final double CONE_RADIUS = 4;
	private static final int CONE_ANGLE = 70;
	private static final double VERTICAL_KB = -0.7;

	public static final String CHARM_VELOCITY = "Meteor Slam Vault Velocity";
	public static final String CHARM_UP_DAMAGE = "Meteor Slam Vault Damage";
	public static final String CHARM_CONE_ANGLE = "Meteor Slam Vault Cone Angle";
	public static final String CHARM_RANGE = "Meteor Slam Vault Range";
	public static final String CHARM_KNOCKBACK = "Meteor Slam Vault Knockback";

	private final double mVaultVelocity;
	private final double mVaultDamage;
	private final double mVaultRadius;
	private final int mConeAngle;
	private final double mVerticalKb;

	// Slam
	public static final double AUTOMATIC_THRESHOLD = 3;
	public static final double MAX_HEIGHT = 7;
	public static final double SLAM_DAMAGE_PER_BLOCK = 3;
	public static final double SLAM_RADIUS = 3;

	public static final String CHARM_THRESHOLD = "Meteor Slam Fall Requirement";
	public static final String CHARM_HEIGHT = "Meteor Slam Max Height";
	public static final String CHARM_SLAM_DAMAGE = "Meteor Slam Damage";
	public static final String CHARM_METEOR_SLAM_RADIUS = "Meteor Slam Radius";

	private final double mThreshold;
	private final double mMaxHeight;
	private final double mSlamRadius;

	// Ground Pound
	public static final double GROUND_POUND_DAMAGE_PER_BLOCK = 1.5;
	public static final double GROUND_POUND_VELOCITY = 2;
	public static final double GROUND_POUND_RADIUS = 1.5;
	public static final int GROUND_POUND_FIRE_DURATION = 5 * Constants.TICKS_PER_SECOND;
	public static final int GROUND_POUND_BLOODLUST_COST = 1;
	public static final double GROUND_POUND_KNOCKBACK = 0.5;
	public static final double GROUND_POUND_SLOWNESS_MULTIPLIER = 0.15;
	public static final int GROUND_POUND_SLOWNESS_DURATION = 40;

	public static final String CHARM_GROUND_POUND_VELOCITY = "Meteor Slam Ground Pound Velocity";
	public static final String CHARM_GROUND_POUND_DAMAGE = "Meteor Slam Ground Pound Damage Per Block Fallen";
	public static final String CHARM_GROUND_POUND_RADIUS = "Meteor Slam Ground Pound Additional Radius";
	public static final String CHARM_GROUND_POUND_FIRE_DURATION = "Meteor Slam Ground Pound Fire Duration";
	public static final String CHARM_GROUND_POUND_BLOODLUST_COST = "Meteor Slam Ground Pound Bloodlust Cost";
	public static final String CHARM_GROUND_POUND_KNOCKBACK = "Meteor Slam Ground Pound Knockback";
	public static final String CHARM_GROUND_POUND_SLOWNESS_MULTIPLIER = "Meteor Slam Ground Pound Slowness Multiplier";
	public static final String CHARM_GROUND_POUND_SLOWNESS_DURATION = "Meteor Slam Ground Pound Slowness Duration";

	private final double mGroundPoundDamage;
	private final double mGroundPoundVelocity;
	private final double mGroundPoundRadius;
	private final int mGroundPoundFireDuration;
	private final int mGroundPoundBloodlustCost;
	private final double mGroundPoundKnockback;
	private final double mGroundPoundSlownessMultiplier;
	private final int mGroundPoundSlownessDuration;

	// Others
	public static final int BLOODLUST_COST = 1;
	private static final int COOLDOWN = 100;

	public static final String CHARM_BLOODLUST_COST = "Meteor Slam Bloodlust Cost";
	public static final String CHARM_COOLDOWN = "Meteor Slam Cooldown";

	private final int mBloodlustCost;

	// Non-charm vars
	private final MeteorSlamCS mCosmetic;
	private final BukkitRunnable mSlamAttackRunner;
	private @Nullable Bloodlust mBloodlust;
	private boolean mHasTouchedGround = false;
	private boolean mGroundPound = false;
	private double mFallFromY = -7050;
	private int mVaultCastTime = 0;
	private int mPoundCastTime = 0;
	private int mSneakTime = 0;

	public static final AbilityInfo<MeteorSlam> INFO =
		new AbilityInfo<>(MeteorSlam.class, "Meteor Slam", MeteorSlam::new)
			.linkedSpell(ClassAbility.METEOR_SLAM)
			.scoreboardId("MeteorSlam")
			.shorthandName("MS")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Swing your weapon to damage mobs and vault yourself upward. Passively generate a slam attack when fallen from great heights.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", MeteorSlam::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)))
			.addTrigger(new AbilityTriggerInfo<>("castgroundpound", "cast ground pound", mSlam -> mSlam.doGroundPound(true), new AbilityTrigger(AbilityTrigger.Key.SWAP).enabled(false)))
			.displayItem(Material.FIRE_CORAL_FAN);

	public MeteorSlam(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mVaultVelocity = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_VELOCITY, VAULT_VELOCITY);
		mVaultDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_UP_DAMAGE, UP_DAMAGE);
		mVaultRadius = CharmManager.getRadius(mPlayer, CHARM_RANGE, CONE_RADIUS);
		mConeAngle = (int) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_CONE_ANGLE, CONE_ANGLE);
		mVerticalKb = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, VERTICAL_KB);

		mThreshold = AUTOMATIC_THRESHOLD + CharmManager.getLevel(mPlayer, CHARM_THRESHOLD);
		mMaxHeight = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEIGHT, MAX_HEIGHT);
		mSlamRadius = CharmManager.getRadius(mPlayer, CHARM_METEOR_SLAM_RADIUS, SLAM_RADIUS);

		mGroundPoundDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_GROUND_POUND_DAMAGE, GROUND_POUND_DAMAGE_PER_BLOCK);
		mGroundPoundVelocity = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_GROUND_POUND_VELOCITY, GROUND_POUND_VELOCITY);
		mGroundPoundRadius = CharmManager.getRadius(mPlayer, CHARM_GROUND_POUND_RADIUS, GROUND_POUND_RADIUS);
		mGroundPoundKnockback = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_GROUND_POUND_KNOCKBACK, GROUND_POUND_KNOCKBACK);
		mGroundPoundBloodlustCost = GROUND_POUND_BLOODLUST_COST + (int) CharmManager.getLevel(mPlayer, CHARM_GROUND_POUND_BLOODLUST_COST);
		mGroundPoundFireDuration = CharmManager.getDuration(mPlayer, CHARM_GROUND_POUND_FIRE_DURATION, GROUND_POUND_FIRE_DURATION);
		mGroundPoundSlownessMultiplier = GROUND_POUND_SLOWNESS_MULTIPLIER + CharmManager.getLevelPercentDecimal(player, CHARM_GROUND_POUND_SLOWNESS_MULTIPLIER);
		mGroundPoundSlownessDuration = CharmManager.getDuration(mPlayer, CHARM_GROUND_POUND_SLOWNESS_DURATION, GROUND_POUND_SLOWNESS_DURATION);

		mBloodlustCost = BLOODLUST_COST + (int) CharmManager.getLevel(mPlayer, CHARM_BLOODLUST_COST);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new MeteorSlamCS());

		Bukkit.getScheduler().runTask(mPlugin, () ->
			mBloodlust = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, Bloodlust.class));

		mSlamAttackRunner = new BukkitRunnable() {
			@Override
			public void run() {
				if (player == null) {
					this.cancel();
					return;
				}
				if (AbilityManager.getManager().getPlayerAbility(player, MeteorSlam.class) == null
					|| player.isDead()
					|| !player.isOnline()) {
					// If reached this point but not silenced, then proceed with cancelling
					// If silenced, only return to not run anything, but don't cancel runnable
					if (!AbilityManager.getManager().getPlayerAbilities(player).isSilenced()) {
						this.cancel();
					}
					return;
				}

				if (!PlayerUtils.isOnGround(player)) {
					updateFallFrom(); // Vanilla fall distance would be 0 if on ground
					groundPoundVelocityCheck();
					doGroundPound(false);
					if (mGroundPound) {
						mCosmetic.onGroundPoundTick(mPlugin, mPlayer.getWorld(), mPlayer.getLocation(), mPlayer);
					}
				} else {
					// Currently on ground

					// If first tick landing, should still have old mFallFromY to calculate using
					// Therefore can damage if eligible
					if (calculateFallDistance() > mThreshold) {
						// Only for checking in LivingEntityDamagedByPlayerEvent below,
						// so doesn't slam twice, since this doesn't yet set fall distance to 0
						MetadataUtils.checkOnceThisTick(plugin, player, SLAM_ONCE_THIS_TICK_METAKEY);
						doSlamAttack(player.getLocation().add(0, 0.15, 0));
					}
					onLanding();
				}
			}
		};
		cancelOnDeath(mSlamAttackRunner.runTaskTimer(plugin, 0, 1));
	}

	public boolean cast() {
		if (isOnCooldown()
			|| ZoneUtils.hasZoneProperty(mPlayer, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)
			|| mBloodlust == null
			|| !mBloodlust.useStacks(mBloodlustCost)) {
			return false;
		}

		putOnCooldown();
		doSlash();
		mPlayer.getScheduler().runDelayed(mPlugin, (task) -> {
			if (!PlayerUtils.isOnGround(mPlayer)) {
				mHasTouchedGround = false;
			}
		}, null, 5);
		mVaultCastTime = Bukkit.getServer().getCurrentTick();

		return true;
	}

	private void doSlash() {
		World world = mPlayer.getWorld();

		mCosmetic.onUpwardSlash(world, mPlayer.getLocation(), mPlayer, mVaultRadius, mConeAngle);

		Location castLocation = mPlayer.getLocation().clone();
		castLocation.setDirection(mPlayer.getLocation().getDirection().setY(0));
		castLocation.setY(castLocation.y() - 3);

		// Mob kb is partially affected by the players initial velocity to make it feel more natural when jumping or using other movement tools
		double playerInitialVelocity = Math.min(0.7, Math.max(0, mPlayer.getVelocity().getY() * 0.65));
		Vector kbVector = new Vector(0, mVerticalKb + playerInitialVelocity, 0);

		Hitbox hitbox = Hitbox.approximateCylinderSegment(castLocation, 6, mVaultRadius, Math.toRadians(mConeAngle) / 2);
		for (LivingEntity target : hitbox.getHitMobs()) {

			double kbMultiplier = 1 - EntityUtils.getAttributeOrDefault(target, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0);
			if (kbMultiplier > 0) {
				target.setVelocity(kbVector.multiply(kbMultiplier));
			}

			DamageUtils.damage(mPlayer, target, DamageEvent.DamageType.MELEE_SKILL, mVaultDamage, ClassAbility.METEOR_SLAM, true);
		}
		Vector dir = mPlayer.getLocation().getDirection().setY(0).normalize().setY(6).normalize();
		Vector velocity = dir.multiply(mVaultVelocity * (mHasTouchedGround ? 1 : VAULT_VELOCITY_PENALTY));
		mPlayer.setVelocity(velocity);

		// TODO: Possible alternative?
		// Jump can cancel y velocity, thus check if this has happened
		mPlayer.getScheduler().runDelayed(mPlugin, (task) -> {
			Vector currVelocity = mPlayer.getVelocity();

			if (velocity.getY() > currVelocity.getY() && !BlockUtils.isBouncy(mPlayer.getLocation().add(0, -1, 0).getBlock().getType())) {
				currVelocity.setY(velocity.getY());
				mPlayer.setVelocity(currVelocity);
			}
		}, null, 2);
	}

	public void groundPoundVelocityCheck() {
		if (isLevelTwo()
			&& mGroundPound
			&& Bukkit.getServer().getCurrentTick() - CAST_DELAY > mPoundCastTime // 5 tick window so ground pound is guarantee
			&& mPlayer.getVelocity().getY() > -mGroundPoundVelocity) {
			mSneakTime = 0;
			mGroundPound = false;
		}
	}

	public boolean doGroundPound(boolean customCast) {
		if (isLevelTwo() && mBloodlust != null && canGroundPound()) {
			boolean customTriggerEnabled = hasCustomTrigger(mPlayer);
			if (!customTriggerEnabled
				&& Bukkit.getServer().getCurrentTick() - CAST_DELAY > mVaultCastTime
				&& mPlayer.isSneaking()
				&& mSneakTime < SNEAK_TIME_REQ) {
				mSneakTime++;
			}
			if (!mGroundPound
				&& ((customTriggerEnabled && customCast) || mSneakTime >= SNEAK_TIME_REQ)) {
				// Seperate if statement to prevent failed cast & consuming stack
				if (mBloodlust.useStacks(mGroundPoundBloodlustCost)) {
					mPlayer.setVelocity(new Vector(0, -mGroundPoundVelocity, 0));
					mCosmetic.onGroundPoundCast(mPlugin, mPlayer.getWorld(), mPlayer.getLocation(), mPlayer);
					mPoundCastTime = Bukkit.getServer().getCurrentTick();
					mGroundPound = true;
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void invalidate() {
		if (mSlamAttackRunner != null) {
			mSlamAttackRunner.cancel();
		}
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		// If there is a mob in range, cancel the fall damage
		if (event.getType() == DamageEvent.DamageType.FALL && !new Hitbox.SphereHitbox(mPlayer.getLocation(), mSlamRadius + (mGroundPound ? mGroundPoundRadius : 0)).getHitMobs().isEmpty()) {
			event.setCancelled(true);
		}
	}

	@Override
	public boolean onDamage(final DamageEvent event, final LivingEntity enemy) {
		if (event.getType() == DamageEvent.DamageType.MELEE
			&& PlayerUtils.isFallingAttack(mPlayer)
			&& calculateFallDistance() > mThreshold) {

			Location loc = enemy.getLocation().add(0, 0.15, 0);

			MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, SLAM_ONCE_THIS_TICK_METAKEY);
			doSlamAttack(loc);
			mCosmetic.onSlamCritical(mPlugin, mPlayer.getWorld(), loc, mPlayer);
			onLanding();

			return true;
		}
		return false;
	}


	// Jumping at the same time cancels slam attack
	private void doSlamAttack(Location location) {
		World world = mPlayer.getWorld();
		double fallDistance = Math.min(mMaxHeight, calculateFallDistance());

		double slamDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SLAM_DAMAGE, fallDistance * SLAM_DAMAGE_PER_BLOCK);
		double slamRadius = mSlamRadius;

		if (mGroundPound) {
			slamRadius += mGroundPoundRadius;
			slamDamage += mGroundPoundDamage * fallDistance;
			mCosmetic.onGroundPoundSlam(mPlugin, world, location, mPlayer, slamRadius);
		}

		for (LivingEntity enemy : new Hitbox.SphereHitbox(location, slamRadius).getHitMobs()) {
			DamageUtils.damage(mPlayer, enemy, DamageEvent.DamageType.MELEE_SKILL, slamDamage, mInfo.getLinkedSpell(), true);
			if (mGroundPound) {
				EntityUtils.applySlow(mPlugin, mGroundPoundSlownessDuration, mGroundPoundSlownessMultiplier, enemy);
				MovementUtils.knockAway(mPlayer, enemy, (float) mGroundPoundKnockback, true);
				EntityUtils.applyFire(mPlugin, mGroundPoundFireDuration, enemy, mPlayer);
			}
		}

		mCosmetic.onSlam(world, location, mPlayer, slamRadius, fallDistance);
	}

	// Since getFallDistance is unreliable (ie does not reset while in bed), we check the distance ourselves.
	// 0 to reset fall distance when in water, vines, etc...
	private void updateFallFrom() {
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

	private boolean canGroundPound() {
		Location loc = mPlayer.getLocation();
		World world = loc.getWorld();
		double halfReq = mThreshold / 2 + 0.125;

		BoundingBox groundPoundHitbox = new BoundingBox().shift(loc.add(0, -halfReq, 0)).expand(0.3, halfReq, 0.3);

		return !NmsUtils.getVersionAdapter().hasCollisionWithBlocks(world, groundPoundHitbox, false);
	}

	private void onLanding() {
		mHasTouchedGround = true;
		mGroundPound = false;
		mFallFromY = -7050;
		mSneakTime = 0;
	}

	private static Description<MeteorSlam> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" vaults you upwards at a velocity of ")
			.add(a -> a.mVaultVelocity, VAULT_VELOCITY)
			.add(" (reduced by ")
			.addPercent(a -> VAULT_VELOCITY_PENALTY, VAULT_VELOCITY_PENALTY)
			.add(" until landing), dealing ")
			.add(a -> a.mVaultDamage, UP_DAMAGE, false, Ability::isLevelOne)
			.add(" melee damage within a ")
			.add(a -> a.mVaultRadius, CONE_RADIUS)
			.add(" block cone and knocking mobs downwards. Falling more than ")
			.add(a -> a.mThreshold, AUTOMATIC_THRESHOLD)
			.add(" blocks generates a slam where you land, dealing ")
			.add(a -> CharmManager.calculateFlatAndPercentValue(a.getPlayer(), CHARM_SLAM_DAMAGE, SLAM_DAMAGE_PER_BLOCK), SLAM_DAMAGE_PER_BLOCK, false, Ability::isLevelOne)
			.add(" melee damage per block fallen, capping at ")
			.add(a -> a.mMaxHeight, MAX_HEIGHT)
			.add(" blocks. Dealing any damage with Meteor Slam cancels all fall damage. \n \n")
			.addCooldown(COOLDOWN)
			.add("\n Cost: ")
			.add(a -> a.mBloodlustCost, BLOODLUST_COST)
			.add("x Bloodlust Stack. ");
	}

	private static Description<MeteorSlam> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Sneaking (or ")
			.addTrigger(1)
			.add(" if enabled) while midair now performs a ground pound attack, lunging you downward at a velocity of ")
			.add(a -> a.mGroundPoundVelocity, GROUND_POUND_VELOCITY)
			.add(". Your next Meteor Slam is amplified: ")
			.add("\n - +")
			.add(a -> a.mGroundPoundDamage, GROUND_POUND_DAMAGE_PER_BLOCK)
			.add(" damage per block fallen.")
			.add("\n - +")
			.add(a -> a.mGroundPoundRadius, GROUND_POUND_RADIUS)
			.add(" block radius.")
			.add("\n - ")
			.addDuration(a -> a.mGroundPoundFireDuration, GROUND_POUND_FIRE_DURATION)
			.add("s of fire.")
			.add("\n - ")
			.addPercent(a -> a.mGroundPoundSlownessMultiplier, GROUND_POUND_SLOWNESS_MULTIPLIER)
			.add(" slowness for ")
			.addDuration(a -> a.mGroundPoundSlownessDuration, GROUND_POUND_SLOWNESS_DURATION)
			.add("s.")
			.add("\n - Knocks back mobs. ")
			.add("\n \n Cost: ")
			.add(a -> a.mGroundPoundBloodlustCost, GROUND_POUND_BLOODLUST_COST)
			.add("x Bloodlust Stack.");
	}

	private static boolean hasCustomTrigger(Player player) {
		AbilityTrigger groundPoundTrigger = AbilityManager.getManager().getCustomTrigger(player, INFO, "castgroundpound");
		return groundPoundTrigger != null && groundPoundTrigger.isEnabled();
	}
}
