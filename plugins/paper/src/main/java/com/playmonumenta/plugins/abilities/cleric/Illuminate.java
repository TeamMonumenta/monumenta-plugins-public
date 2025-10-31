package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.IlluminateCS;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.HashSet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public class Illuminate extends Ability {
	private static final ClassAbility ILLUMINATE_DOT_ABILITY = ClassAbility.ILLUMINATE_DOT;
	private static final int COOLDOWN_1 = TICKS_PER_SECOND * 14;
	private static final int COOLDOWN_2 = TICKS_PER_SECOND * 12;
	private static final int ILLUMINATE_MAX_RANGE = 24;
	private static final double ILLUMINATE_VELOCITY = 1.4;
	private static final double ILLUMINATE_HITBOX_RADIUS = 0.8;
	private static final double ILLUMINATE_TRAIL_WIDTH = 3.5;
	private static final int ILLUMINATE_TRAIL_DURATION_1 = TICKS_PER_SECOND * 6;
	private static final int ILLUMINATE_TRAIL_DURATION_2 = TICKS_PER_SECOND * 8;
	private static final int ILLUMINATE_BUFF_DURATION = TICKS_PER_SECOND * 4;
	private static final double ILLUMINATE_SPEED_BUFF = 0.20;
	private static final double ILLUMINATE_STRENGTH_BUFF = 0.10;
	private static final double ILLUMINATE_DAMAGE_1 = 11;
	private static final double ILLUMINATE_DAMAGE_2 = 15;
	private static final double ILLUMINATE_RADIUS = 4.5;
	private static final float ILLUMINATE_KNOCKBACK = 0.5f;
	private static final double ILLUMINATE_ENHANCE_RADIUS = 6.0;
	private static final double ILLUMINATE_ENHANCE_DAMAGE = 1;
	private static final int ILLUMINATE_ENHANCE_COOLDOWN = TICKS_PER_SECOND / 2;

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
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
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

	private @Nullable BukkitRunnable mCastRunnable;
	private @Nullable BukkitRunnable mEffectsRunnable;
	private @Nullable BukkitRunnable mLayTrailRunnable;
	private @Nullable BukkitRunnable mSanctifiedZoneRunnable;
	private @Nullable Location mEnhanceZone; // used for enhance cosmetic

	public Illuminate(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new IlluminateCS());

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

		mCastRunnable = null;
		mEffectsRunnable = null;
		mLayTrailRunnable = null;
		mSanctifiedZoneRunnable = null;
		mEnhanceZone = null; // for cosmetic
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		putOnCooldown();
		mCosmetic.castEffects(mPlayer);

		final Location startLoc = mPlayer.getLocation();

		ClientModHandler.updateAbility(mPlayer, this);

		final Location mLoc = mPlayer.getEyeLocation();
		final Vector mIncrement = mLoc.getDirection().multiply(mMoveSpeed);

		mPlayersInZone.clear();
		mMobsInZone.clear();

		mCastRunnable = new BukkitRunnable() {
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

					final Hitbox hitbox = new Hitbox.SphereHitbox(mLoc, ILLUMINATE_HITBOX_RADIUS);
					if (!hitbox.getHitMobs().isEmpty() || !mLoc.isChunkLoaded() ||
						LocationUtils.collidesWithBlocks(BoundingBox.of(mLoc.clone().add(ILLUMINATE_HITBOX_RADIUS / 2, ILLUMINATE_HITBOX_RADIUS / 2, ILLUMINATE_HITBOX_RADIUS / 2),
								mLoc.clone().add(-ILLUMINATE_HITBOX_RADIUS / 2, -ILLUMINATE_HITBOX_RADIUS / 2, -ILLUMINATE_HITBOX_RADIUS / 2)),
							mLoc.getWorld(), false) || mLoc.distance(startLoc) > mMaxRange) {

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
		};
		cancelOnDeath(mCastRunnable.runTaskTimer(mPlugin, 0, 1));

		mEffectsRunnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				for (final Player player : mPlayersInZone) {
					mPlugin.mEffectManager.addEffect(player, "IlluminateSpeedEffect",
						new PercentSpeed(ILLUMINATE_BUFF_DURATION, mSpeedBuff, "IlluminateSpeedEffect")
							.deleteOnAbilityUpdate(true));

					if (isLevelTwo()) {
						mPlugin.mEffectManager.addEffect(player, "IlluminateStrengthEffect",
							new PercentDamageDealt(ILLUMINATE_BUFF_DURATION, mStrengthBuff).deleteOnAbilityUpdate(true));
					}
				}

				if (isEnhanced() && mTicks % ILLUMINATE_ENHANCE_COOLDOWN == 0) {
					for (final LivingEntity mob : mMobsInZone) {
						DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, mEnhanceDamage, ILLUMINATE_DOT_ABILITY, true);
						mCosmetic.enhanceTickDamageEffect(mPlayer, mob);
					}
				}

				mPlayersInZone.clear();
				mMobsInZone.clear();

				mTicks++;
				if (mTicks > mTrailDuration) {
					this.cancel();
				}
			}
		};
		cancelOnDeath(mEffectsRunnable.runTaskTimer(mPlugin, 0, 1));

		return true;
	}

	private void layTrail(final Location loc, final double radius, final int maxDuration, final Vector direction) {
		mLayTrailRunnable = new BukkitRunnable() {
			int mTrailTicks = 0;

			final Location mTrailLoc = loc.clone().subtract(0, 1, 0);

			@Override
			public void run() {
				mPlayersInZone.addAll(PlayerUtils.playersInRange(mTrailLoc, radius, true));
				mMobsInZone.addAll(EntityUtils.getNearbyMobs(mTrailLoc, radius));

				mCosmetic.trailEffects(mPlayer, mTrailLoc, radius * 0.9, direction, mTrailTicks, maxDuration, mEnhanceZone, mEnhanceRadius);

				mTrailTicks++;
				if (mTrailTicks > maxDuration) {
					this.cancel();
				}
			}
		};
		cancelOnDeath(mLayTrailRunnable.runTaskTimer(mPlugin, 0, 1));
	}

	private void doExplosion(final Location loc, final double damage, final double radius, final float knockback) {
		new Hitbox.SphereHitbox(loc, radius).getHitMobs().forEach(mob -> {
			DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, damage, mInfo.getLinkedSpell(), true);
			MovementUtils.knockAway(loc, mob, knockback, knockback, true);
			mCosmetic.explosionHitEffects(mPlayer, mob);
		});
	}

	private void placeSanctifiedZone(final Location loc, final double radius, final double maxDuration) {
		mEnhanceZone = loc;
		mSanctifiedZoneRunnable = new BukkitRunnable() {
			int mZoneTicks = 0;
			final Location mZoneLoc = loc.clone().subtract(0, 1, 0);

			@Override
			public void run() {
				mPlayersInZone.addAll(PlayerUtils.playersInRange(mZoneLoc, radius, true));
				mMobsInZone.addAll(EntityUtils.getNearbyMobs(mZoneLoc, radius));
				mCosmetic.sanctifiedZoneEffects(mPlayer, mZoneLoc, radius, mZoneTicks, maxDuration);

				mZoneTicks++;
				if (mZoneTicks > maxDuration) {
					mEnhanceZone = null;
					this.cancel();
				}
			}
		};
		cancelOnDeath(mSanctifiedZoneRunnable.runTaskTimer(mPlugin, 0, 1));
	}

	@Override
	public void invalidate() {
		if (mCastRunnable != null && !mCastRunnable.isCancelled()) {
			mCastRunnable.cancel();
		}

		if (mEffectsRunnable != null && !mEffectsRunnable.isCancelled()) {
			mEffectsRunnable.cancel();
		}

		if (mLayTrailRunnable != null && !mLayTrailRunnable.isCancelled()) {
			mLayTrailRunnable.cancel();
		}

		if (mSanctifiedZoneRunnable != null && !mSanctifiedZoneRunnable.isCancelled()) {
			mSanctifiedZoneRunnable.cancel();
		}
	}

	private static Description<Illuminate> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to fire a holy projectile that travels for ")
			.add(a -> a.mMaxRange, ILLUMINATE_MAX_RANGE)
			.add(" blocks, leaving behind a ")
			.add(a -> a.mTrailWidth, ILLUMINATE_TRAIL_WIDTH)
			.add(" block wide trail that lasts for ")
			.addDuration(a -> a.mTrailDuration, ILLUMINATE_TRAIL_DURATION_1, false, Ability::isLevelOne)
			.add(" seconds and grants ")
			.addPercent(a -> a.mSpeedBuff, ILLUMINATE_SPEED_BUFF)
			.add(" speed to all players inside it. Buffs linger for ")
			.addDuration(ILLUMINATE_BUFF_DURATION)
			.add(" seconds upon leaving the area. Upon hitting a mob, block, or reaching its max distance, the projectile explodes, dealing ")
			.add(a -> a.mDamage, ILLUMINATE_DAMAGE_1, false, Ability::isLevelOne)
			.add(" magic damage to mobs within ")
			.add(a -> a.mRadius, ILLUMINATE_RADIUS)
			.add(" blocks and knocking them away.")
			.addCooldown(COOLDOWN_1, Ability::isLevelOne);
	}

	private static Description<Illuminate> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The damage is increased to ")
			.add(a -> a.mDamage, ILLUMINATE_DAMAGE_2, false, Ability::isLevelTwo)
			.add(", and the trail's duration is increased to ")
			.addDuration(a -> a.mTrailDuration, ILLUMINATE_TRAIL_DURATION_2, false, Ability::isLevelTwo)
			.add(" seconds. Additionally, players within the trail also gain ")
			.addPercent(a -> a.mStrengthBuff, ILLUMINATE_STRENGTH_BUFF)
			.add(" strength.")
			.addCooldown(COOLDOWN_2, Ability::isLevelTwo);
	}

	private static Description<Illuminate> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("A sanctified area is placed wherever Illuminate explodes, granting the same effects as the trail within a ")
			.add(a -> a.mEnhanceRadius, ILLUMINATE_ENHANCE_RADIUS)
			.add(" block radius and lasting for the same duration. Enemies within Illuminate's trail take ")
			.add(a -> a.mEnhanceDamage, ILLUMINATE_ENHANCE_DAMAGE)
			.add(" magic damage every ")
			.addDuration(ILLUMINATE_ENHANCE_COOLDOWN)
			.add(" seconds.");
	}
}
