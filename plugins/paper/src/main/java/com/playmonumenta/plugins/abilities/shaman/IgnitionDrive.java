package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.IgnitionDriveCS;
import com.playmonumenta.plugins.effects.DamageImmunity;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class IgnitionDrive extends Ability {
	private static final int COOLDOWN_1 = 18 * 20;
	private static final int COOLDOWN_2 = 15 * 20;
	private static final int DAMAGE_1 = 11;
	private static final int DAMAGE_2 = 15;
	private static final int RADIUS_1 = 3;
	private static final int RADIUS_2 = 4;
	private static final int FIRE_DURATION = 4 * 20;
	private static final double LAUNCH_DISTANCE_1 = 0.8;
	private static final double LAUNCH_DISTANCE_2 = 1.0;
	private static final int FALL_IMMUNITY_DURATION = 10 * 20;
	private static final int STUN_LIMIT_1 = 2;
	private static final int STUN_LIMIT_2 = 3;
	private static final int STUN_DURATION = 20;
	private static final double LAUNCH_STUN_RADIUS = 2.5;
	private static final double ENHANCE_UPPER_THRESHOLD = 0.75;
	private static final double ENHANCE_COOLDOWN_REFRESH = 0.25;
	private static final double ENHANCE_LOWER_THRESHOLD = 0.75;
	private static final double ENHANCE_MAGIC_DMG_BOOST = 0.25;
	private static final int ENHANCE_MAGIC_DMG_DURATION = 5 * 20;
	private static final String ENHANCE_DMG_EFFECT = "IgnitionDriveEnhancementMagicDmgBoost";
	private static final int ENHANCE_STUN_DURATION = 2 * 20;

	public static final String CHARM_COOLDOWN = "Ignition Drive Cooldown";
	public static final String CHARM_DAMAGE = "Ignition Drive Damage";
	public static final String CHARM_RADIUS = "Ignition Drive Radius";
	public static final String CHARM_FIRE_DURATION = "Ignition Drive Fire Duration";
	public static final String CHARM_LAUNCH_DISTANCE = "Ignition Drive Launch Distance";
	public static final String CHARM_STUN_LIMIT = "Ignition Drive Stun Limit";
	public static final String CHARM_STUN_DURATION = "Ignition Drive Stun Duration";
	public static final String CHARM_ENHANCE_UPPER_THRESHOLD = "Ignition Drive Enhancement Upper Health Threshold";
	public static final String CHARM_ENHANCE_COOLDOWN_REFRESH = "Ignition Drive Enhancement Cooldown Refresh";
	public static final String CHARM_ENHANCE_LOWER_THRESHOLD = "Ignition Drive Enhancement Lower Health Threshold";
	public static final String CHARM_ENHANCE_MAGIC_DMG = "Ignition Drive Enhancement Magic Damage";
	public static final String CHARM_ENHANCE_MAGIC_DURATION = "Ignition Drive Enhancement Magic Damage Duration";
	public static final String CHARM_ENHANCE_STUN_DURATION = "Ignition Drive Enhancement Stun Duration";

	public static final AbilityInfo<IgnitionDrive> INFO =
		new AbilityInfo<>(IgnitionDrive.class, "Ignition Drive", IgnitionDrive::new)
			.linkedSpell(ClassAbility.IGNITION_DRIVE)
			.scoreboardId("IgnitionDrive")
			.shorthandName("ID")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", IgnitionDrive::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE, AbilityTrigger.KeyOptions.NO_BLOCKS, AbilityTrigger.KeyOptions.NO_POTION, AbilityTrigger.KeyOptions.NO_FOOD)))
			.addAltPresetTrigger(new AbilityTriggerInfo<>("cast", "cast", IgnitionDrive::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false).onGround(false)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE, AbilityTrigger.KeyOptions.NO_BLOCKS, AbilityTrigger.KeyOptions.NO_POTION, AbilityTrigger.KeyOptions.NO_FOOD)))
			.simpleDescription("Harness thunder and flames to launch forth, dealing damage and lighting mobs on fire.")
			.displayItem(Material.FIRE_CHARGE);

	private final double mDamage;
	private final double mRadius;
	private final int mFireDuration;
	private final double mLaunchDistance;
	private final int mStunLimit;
	private final int mStunDuration;
	private final double mEnhanceUpperHealthThreshold;
	private final double mEnhanceCDR;
	private final double mEnhanceLowerHealthThreshold;
	private final double mEnhanceMagicDmg;
	private final int mEnhanceMagicDmgDuration;
	private final int mEnhanceStunDuration;
	private final IgnitionDriveCS mCosmetic;

	public IgnitionDrive(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, isLevelOne() ? RADIUS_1 : RADIUS_2);
		mFireDuration = CharmManager.getDuration(mPlayer, CHARM_FIRE_DURATION, FIRE_DURATION);
		mLaunchDistance = CharmManager.getRadius(mPlayer, CHARM_LAUNCH_DISTANCE, isLevelOne() ? LAUNCH_DISTANCE_1 : LAUNCH_DISTANCE_2);
		mStunLimit = (isLevelOne() ? STUN_LIMIT_1 : STUN_LIMIT_2) + (int) CharmManager.getLevel(player, CHARM_STUN_LIMIT);
		mStunDuration = CharmManager.getDuration(mPlayer, CHARM_STUN_DURATION, STUN_DURATION);
		mEnhanceUpperHealthThreshold = ENHANCE_UPPER_THRESHOLD + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ENHANCE_UPPER_THRESHOLD);
		mEnhanceCDR = ENHANCE_COOLDOWN_REFRESH + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ENHANCE_COOLDOWN_REFRESH);
		mEnhanceLowerHealthThreshold = ENHANCE_LOWER_THRESHOLD + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ENHANCE_LOWER_THRESHOLD);
		mEnhanceMagicDmg = ENHANCE_MAGIC_DMG_BOOST + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ENHANCE_MAGIC_DMG);
		mEnhanceMagicDmgDuration = CharmManager.getDuration(mPlayer, CHARM_ENHANCE_MAGIC_DURATION, ENHANCE_MAGIC_DMG_DURATION);
		mEnhanceStunDuration = CharmManager.getDuration(mPlayer, CHARM_ENHANCE_STUN_DURATION, ENHANCE_STUN_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new IgnitionDriveCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		if (ZoneUtils.hasZoneProperty(mPlayer, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
			return false;
		}

		// Apply damage immunity from cast to landing
		mPlugin.mEffectManager.addEffect(mPlayer, "IgnitionDriveFallImmunity",
			new DamageImmunity(FALL_IMMUNITY_DURATION, EnumSet.of(DamageEvent.DamageType.FALL)).deleteOnAbilityUpdate(true));

		putOnCooldown();

		double playerHealthRatio = 1;
		if (isEnhanced()) {
			playerHealthRatio = mPlayer.getHealth() / EntityUtils.getMaxHealth(mPlayer);
			if (playerHealthRatio > mEnhanceUpperHealthThreshold) {
				mPlugin.mTimers.updateCooldownPercent(mPlayer, Objects.requireNonNull(INFO.getLinkedSpell()), mEnhanceCDR);
			}

			if (playerHealthRatio < mEnhanceLowerHealthThreshold) {
				mPlugin.mEffectManager.addEffect(mPlayer, ENHANCE_DMG_EFFECT, new PercentDamageDealt(mEnhanceMagicDmgDuration, mEnhanceMagicDmg)
					.damageTypes(EnumSet.of(DamageEvent.DamageType.MAGIC)).deleteOnAbilityUpdate(true).deleteOnLogout(true));
			}
		}

		Location startLocation = mPlayer.getLocation();

		// Deal damage at start location
		dealDamageAtLocation(startLocation, isEnhanced() && playerHealthRatio < mEnhanceLowerHealthThreshold, true);

		// Launch player forward using TacticalManeuver approach
		Vector direction = mPlayer.getLocation().getDirection();
		direction.multiply(mLaunchDistance); // Scale based on launch distance
		mPlayer.setVelocity(direction.setY(direction.getY() + 0.4));

		// Launch cosmetic effect
		mCosmetic.ignitionDriveLaunchEffect(mPlayer);

		// Start landing detection after 1 second with 10 second timeout
		cancelOnDeath(new BukkitRunnable() {
			int mTicks = 0;
			int mEnemiesStunned = 0;
			final List<LivingEntity> mMobsNotHit = EntityUtils.getNearbyMobs(mPlayer.getLocation(), 16);

			@Override
			public void run() {
				// STUN MOBS // If we don't stun mobs then you would take damage so easily from just moving around, want it to be a viable combat tool
				// Uses same approach as Wind Walk
				Iterator<LivingEntity> iterator = mMobsNotHit.iterator();
				while (iterator.hasNext() && mEnemiesStunned < mStunLimit) {
					LivingEntity mob = iterator.next();

					if (mob.getLocation().distance(mPlayer.getLocation()) < LAUNCH_STUN_RADIUS && !EntityUtils.isStunned(mob)) {
						EntityUtils.applyStun(mPlugin, mStunDuration, mob);
						mCosmetic.ignitionDriveStunMobSFX(mPlayer);
						iterator.remove();
						mEnemiesStunned++;
					}
				}

				// Check if player has landed or timeout after 10 seconds (200 ticks total)
				Block block = mPlayer.getLocation().getBlock();
				if (mTicks > 1 && (PlayerUtils.isOnGroundOrMountIsOnGround(mPlayer) || BlockUtils.isWaterlogged(block) || block.getType() == Material.LAVA || BlockUtils.isClimbable(block) || mTicks >= FALL_IMMUNITY_DURATION)) {
					// Deal damage at landing location
					dealDamageAtLocation(mPlayer.getLocation(), false, false);

					// Apply damage immunity from cast to landing
					mPlugin.mEffectManager.clearEffects(mPlayer, "IgnitionDriveFallImmunity");

					this.cancel();
					return;
				}

				// Cancel if player is dead, offline, or chunk not loaded
				if (mPlayer.isDead() || !mPlayer.isOnline() || !mPlayer.getLocation().isChunkLoaded()) {
					this.cancel();
					return;
				}

				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1));

		return true;
	}

	private void dealDamageAtLocation(Location location, boolean enhanceStun, boolean startLaunch) {
		List<LivingEntity> mobs = new Hitbox.SphereHitbox(location, mRadius).getHitMobs();
		for (LivingEntity mob : mobs) {
			DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true, false);
			// Set mob on fire
			mob.setFireTicks(mFireDuration);

			if (enhanceStun) {
				EntityUtils.applyStun(mPlugin, mEnhanceStunDuration, mob);
			}
		}

		// Cosmetic effects
		mCosmetic.ignitionDriveDamageEffect(mPlayer, location, mRadius, startLaunch);
	}

	private static Description<IgnitionDrive> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Launch forwards, dealing damage to and")
			.addLine("igniting nearby mobs upon launching.")
			.addLine()
			.addLine("While midair, you stun up to %d1 mobs in your")
				.statValues(stat(a -> a.mStunLimit, STUN_LIMIT_1))
			.addLine("path, and you damage and ignite mobs again")
			.addLine("upon landing.")
			.addLine()
			.addStat("Damage: %d1 (s)")
				.statValues(stat(a -> a.mDamage, DAMAGE_1))
			.addStat("Radius: %r1")
				.statValues(stat(a -> a.mRadius, RADIUS_1))
			.addStat("Effect: Fire for %t")
				.statValues(stat(a -> a.mFireDuration, FIRE_DURATION))
			.addStat("Effect: Stun for %t")
				.statValues(stat(a -> a.mStunDuration, STUN_DURATION))
			.addStat("Cooldown: %t1")
				.statValues(cooldown(COOLDOWN_1))
			.addDashedLine();
	}

	private static Description<IgnitionDrive> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Ignition Drive*'s damage,").styles(UNDERLINED)
			.addLine("radius, maximum mobs stunned, and")
			.addLine("launch velocity.")
			.addLine()
			.addStatComparison("Damage: %d1 -> %d2 (s)")
				.statValues(stat(DAMAGE_1), stat(a -> a.mDamage, DAMAGE_2))
			.addStatComparison("Radius: %r1 -> %r2")
				.statValues(stat(RADIUS_1), stat(a -> a.mRadius, RADIUS_2))
			.addStatComparison("Max Mobs Stunned: %d1 -> %d2")
				.statValues(stat(STUN_LIMIT_1), stat(a -> a.mStunLimit, STUN_LIMIT_2))
			.addStat("Velocity: +%p")
				.statValues(stat((LAUNCH_DISTANCE_2 - LAUNCH_DISTANCE_1) / LAUNCH_DISTANCE_1))
			.addDashedLine();
	}

	private static Description<IgnitionDrive> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Casting *Ignition Drive* while above %p HP").styles(UNDERLINED)
				.statValues(stat(a -> a.mEnhanceUpperHealthThreshold, ENHANCE_UPPER_THRESHOLD))
			.addLine("reduces its cooldown.")
			.addLine()
			.addStat("Cooldown Reduction: %p")
				.statValues(stat(a -> a.mEnhanceCDR, ENHANCE_COOLDOWN_REFRESH))
			.addLine()
			.addLine("Casting *Ignition Drive* while below %p HP").styles(UNDERLINED)
				.statValues(stat(a -> a.mEnhanceLowerHealthThreshold, ENHANCE_LOWER_THRESHOLD))
			.addLine("grants you increased magic damage and")
			.addLine("stuns mobs in the launch area.")
			.addLine()
			.addStat("Effect: +%p Magic Damage for %t")
			.statValues(stat(a -> a.mEnhanceMagicDmg, ENHANCE_MAGIC_DMG_BOOST), stat(a -> a.mEnhanceMagicDmgDuration, ENHANCE_MAGIC_DMG_DURATION))
			.addStat("Effect: Stun for %t")
			.statValues(stat(a -> a.mEnhanceStunDuration, ENHANCE_STUN_DURATION))
			.addDashedLine();
	}
}
