package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.*;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.IlluminateCS;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.*;
import java.util.HashSet;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;


public class Illuminate extends Ability {

	public static final int COOLDOWN_1 = 14 * 20;
	public static final int COOLDOWN_2 = 12 * 20;
	public static final int ILLUMINATE_MAX_RANGE = 24;
	public static final double ILLUMINATE_VELOCITY = 1.4;
	public static final double ILLUMINATE_HITBOX_RADIUS = 0.8;
	public static final double ILLUMINATE_TRAIL_WIDTH = 3.5;
	public static final int ILLUMINATE_TRAIL_DURATION_1 = 6 * 20;
	public static final int ILLUMINATE_TRAIL_DURATION_2 = 8 * 20;
	public static final double ILLUMINATE_SPEED_BUFF = 0.20;
	public static final double ILLUMINATE_STRENGTH_BUFF = 0.10;
	public static final double ILLUMINATE_DAMAGE_1 = 8;
	public static final double ILLUMINATE_DAMAGE_2 = 13;
	public static final double ILLUMINATE_RADIUS = 4.5;
	public static final float ILLUMINATE_KNOCKBACK = 0.5f;
	public static final double ILLUMINATE_ENHANCE_RADIUS = 6.0;
	public static final double ILLUMINATE_ENHANCE_DAMAGE = 1;

	public static final String CHARM_COOLDOWN = "Illuminate Cooldown";
	public static final String CHARM_RANGE = "Illuminate Max Range";
	public static final String CHARM_VELOCITY = "Illuminate Velocity";
	public static final String CHARM_TRAIL_WIDTH = "Illuminate Trail Width";
	public static final String CHARM_TRAIL_DURATION = "Illuminate Trail Duration";
	public static final String CHARM_SPEED_BUFF = "Illuminate Speed Amplifier";
	public static final String CHARM_STRENGTH_BUFF = "Illuminate Strength Amplifier";
	public static final String CHARM_DAMAGE = "Illuminate Damage";
	public static final String CHARM_RADIUS = "Illuminate Radius";
	public static final String CHARM_KNOCKBACK = "Illuminate Knockback";
	public static final String CHARM_ENHANCE_RADIUS = "Illuminate Enhancement Radius";
	public static final String CHARM_ENHANCE_DAMAGE = "Illuminate Enhancement Damage";

	public static final AbilityInfo<Illuminate> INFO =
		new AbilityInfo<>(Illuminate.class, "Illuminate", Illuminate::new)
			.linkedSpell(ClassAbility.ILLUMINATE)
			.scoreboardId("Illuminate")
			.shorthandName("IL")
			.descriptions(
				String.format("Pressing the drop key will fire a holy projectile that travels for %s blocks, " +
					"leaving behind a %s-block wide trail that lasts for %ss and grants +%s%% Speed to all players inside it. " +
					"Buffs linger for 4s upon leaving the area. " +
					"Upon hitting a mob, block, or reaching its max distance, the projectile explodes, dealing %s magic damage " +
					"in a %s-block radius to all mobs and knocking them away. Cooldown: %ss.",
					StringUtils.formatDecimal(ILLUMINATE_MAX_RANGE),
					StringUtils.formatDecimal(ILLUMINATE_TRAIL_WIDTH),
					StringUtils.ticksToSeconds(ILLUMINATE_TRAIL_DURATION_1),
					StringUtils.multiplierToPercentage(ILLUMINATE_SPEED_BUFF),
					StringUtils.formatDecimal(ILLUMINATE_DAMAGE_1),
					StringUtils.formatDecimal(ILLUMINATE_RADIUS),
					StringUtils.ticksToSeconds(COOLDOWN_1)
				),
				String.format("The damage is increased from %s to %s, and the trail's duration is increased from %ss to %ss. " +
					"Additionally, players within the trail also gain +%s%% Strength. Cooldown: %ss.",
					StringUtils.formatDecimal(ILLUMINATE_DAMAGE_1),
					StringUtils.formatDecimal(ILLUMINATE_DAMAGE_2),
					StringUtils.ticksToSeconds(ILLUMINATE_TRAIL_DURATION_1),
					StringUtils.ticksToSeconds(ILLUMINATE_TRAIL_DURATION_2),
					StringUtils.multiplierToPercentage(ILLUMINATE_STRENGTH_BUFF),
					StringUtils.ticksToSeconds(COOLDOWN_2)
				),
				String.format("A sanctified area is placed wherever Illuminate explodes, granting the same effects as the trail within a %s-block radius and lasting for the same duration. " +
					"Enemies within Illuminate's trail take %s magic damage every 0.5s.",
					StringUtils.formatDecimal(ILLUMINATE_ENHANCE_RADIUS),
					StringUtils.formatDecimal(ILLUMINATE_ENHANCE_DAMAGE)
				))
			.simpleDescription("Fire an explosive projectile forwards while leaving behind a trail that buffs allies.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Illuminate::cast, new AbilityTrigger(AbilityTrigger.Key.DROP)))
			.displayItem(Material.YELLOW_CANDLE);

	private final IlluminateCS mCosmetic;

	private final HashSet<Player> mPlayersInZone;
	private final HashSet<LivingEntity> mMobsInZone;

	private final double mMoveSpeed;
	private final double mMaxRange;
	private final int mTrailDuration;
	private final double mTrailWidth;
	private final double mDamage;
	private final double mRadius;
	private final float mKnockback;
	private final double mSpeedBuff;
	private final double mStrengthBuff;
	private final double mEnhanceRadius;
	private final double mEnhanceDamage;

	// used for enhance cosmetic
	private @Nullable Location mEnhanceZone;


	public Illuminate(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new IlluminateCS());

		mPlayersInZone = new HashSet<>();
		mMobsInZone = new HashSet<>();

		mMoveSpeed = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_VELOCITY, ILLUMINATE_VELOCITY);
		mMaxRange = CharmManager.getDuration(mPlayer, CHARM_RANGE, ILLUMINATE_MAX_RANGE);
		mTrailDuration = CharmManager.getDuration(mPlayer, CHARM_TRAIL_DURATION, isLevelOne() ? ILLUMINATE_TRAIL_DURATION_1 : ILLUMINATE_TRAIL_DURATION_2);
		mTrailWidth = CharmManager.getRadius(mPlayer, CHARM_TRAIL_WIDTH, ILLUMINATE_TRAIL_WIDTH);

		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? ILLUMINATE_DAMAGE_1 : ILLUMINATE_DAMAGE_2);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, ILLUMINATE_RADIUS);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, ILLUMINATE_KNOCKBACK);

		mSpeedBuff = ILLUMINATE_SPEED_BUFF + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED_BUFF);
		mStrengthBuff = ILLUMINATE_STRENGTH_BUFF + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_STRENGTH_BUFF);

		// enhancement
		mEnhanceRadius = CharmManager.getRadius(mPlayer, CHARM_ENHANCE_RADIUS, ILLUMINATE_ENHANCE_RADIUS);
		mEnhanceDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCE_DAMAGE, ILLUMINATE_ENHANCE_DAMAGE);

		// for cosmetic
		mEnhanceZone = null;
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		putOnCooldown();
		mCosmetic.castEffects(mPlayer);

		Location startLoc = mPlayer.getLocation();

		ClientModHandler.updateAbility(mPlayer, this);

		final Location mLoc = mPlayer.getEyeLocation();
		final Vector mIncrement = mLoc.getDirection().multiply(mMoveSpeed);

		mPlayersInZone.clear();
		mMobsInZone.clear();

		cancelOnDeath(new BukkitRunnable() {
			@Override
			public void run() {
				if (!mPlayer.getWorld().equals(mLoc.getWorld())) {
					cancel();
					return;
				}

				// lay trail
				layTrail(mLoc, mTrailWidth / 2.0, mTrailDuration, mIncrement);
				// check at half step and full step as velocity is high enough to phase through things sometimes
				for (int i = 0; i < 4; i++) {
					mLoc.add(mIncrement.clone().multiply(0.25));

					Hitbox hitbox = new Hitbox.SphereHitbox(mLoc, ILLUMINATE_HITBOX_RADIUS);
					if (!hitbox.getHitMobs().isEmpty() || !mLoc.isChunkLoaded() ||
						LocationUtils.collidesWithSolid(mLoc) || mLoc.distance(startLoc) > mMaxRange) {

						doExplosion(mLoc, mDamage, mRadius, mKnockback);
						if (isEnhanced()) {
							placeSanctifiedZone(mLoc, mEnhanceRadius, mTrailDuration);
						}

						mCosmetic.projectileExplosionEffects(mPlayer, mLoc);
						this.cancel();
						break;
					}
				}

				mCosmetic.projectileEffects(mPlayer, mLoc);
			}
		}.runTaskTimer(mPlugin, 0, 1));

		cancelOnDeath(new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				for (Player player : mPlayersInZone) {
					mPlugin.mEffectManager.addEffect(player, "IlluminateSpeedEffect", new PercentSpeed(80, mSpeedBuff, "IlluminateSpeedEffect"));
					if (isLevelTwo()) {
						mPlugin.mEffectManager.addEffect(player, "IlluminateStrengthEffect", new PercentDamageDealt(80, mStrengthBuff));
					}
				}

				if (isEnhanced() && mTicks % 10 == 0) {
					for (LivingEntity mob : mMobsInZone) {
						DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, mEnhanceDamage, mInfo.getLinkedSpell(), true);
						mCosmetic.enhanceTickDamageEffect(mPlayer, mob);
					}
				}

				mPlayersInZone.clear();
				mMobsInZone.clear();

				mTicks += 1;
				if (mTicks > mTrailDuration) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	public void layTrail(Location loc, double radius, int maxDuration, Vector direction) {
		cancelOnDeath(new BukkitRunnable() {
			int mTrailTicks = 0;

			final Location mTrailLoc = loc.clone().subtract(0, 1, 0);
			@Override
			public void run() {
				List<Player> players = PlayerUtils.playersInRange(mTrailLoc, radius, true);
				mPlayersInZone.addAll(players);

				List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mTrailLoc, radius);
				mMobsInZone.addAll(mobs);

				mCosmetic.trailEffects(mPlayer, mTrailLoc, radius * 0.9, direction, mTrailTicks, maxDuration, mEnhanceZone, mEnhanceRadius);

				mTrailTicks += 1;
				if (mTrailTicks > maxDuration) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	public void doExplosion(Location loc, double damage, double radius, float knockback) {

		List<LivingEntity> hitMobs = new Hitbox.SphereHitbox(loc, radius).getHitMobs();
		for (LivingEntity mob : hitMobs) {
			DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, damage, mInfo.getLinkedSpell(), true);
			MovementUtils.knockAway(loc, mob, knockback, knockback, true);
			mCosmetic.explosionHitEffects(mPlayer, mob);
		}
	}

	public void placeSanctifiedZone(Location loc, double radius, double maxDuration) {
		mEnhanceZone = loc;
		cancelOnDeath(new BukkitRunnable() {
			int mZoneTicks = 0;
			final Location mZoneLoc = loc.clone().subtract(0, 1, 0);
			@Override
			public void run() {
				List<Player> players = PlayerUtils.playersInRange(mZoneLoc, radius, true);
				mPlayersInZone.addAll(players);

				List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mZoneLoc, radius);
				mMobsInZone.addAll(mobs);

				mCosmetic.sanctifiedZoneEffects(mPlayer, mZoneLoc, radius, mZoneTicks);

				mZoneTicks += 1;
				if (mZoneTicks > maxDuration) {
					mEnhanceZone = null;
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

}
