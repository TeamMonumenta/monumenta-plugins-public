package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.WindBombCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Flying;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class WindBomb extends Ability {
	private static final int DURATION = Constants.TICKS_PER_SECOND * 4;
	private static final double WEAKEN_EFFECT = 0.2;
	private static final double SLOW_EFFECT = 0.2;
	private static final double LAUNCH_VELOCITY = 1.2;
	private static final int SLOW_FALL_POTENCY = 0;
	private static final int COOLDOWN_1 = Constants.TICKS_PER_SECOND * 15;
	private static final int COOLDOWN_2 = Constants.TICKS_PER_SECOND * 10;
	private static final int STUN_DURATION = Constants.TICKS_PER_SECOND;
	private static final double DAMAGE_FRACTION_1 = 0.4;
	private static final double DAMAGE_FRACTION_2 = 0.5;
	private static final double MIDAIR_DAMAGE_BONUS = 0.2;
	private static final int RADIUS = 3;
	private static final double VELOCITY = 1.5;
	private static final String MIDAIR_DAMAGE_METAKEY = "WindBombMidairDamageBonus";

	private static final double VORTEX_HEIGHT = 3;
	private static final int PULL_INTERVAL = 4;
	private static final double PULL_VELOCITY = 0.2;
	private static final double PULL_RADIUS = 10;
	private static final int PULL_DURATION = (Constants.TICKS_PER_SECOND * 3);
	private static final double PULL_RATIO = 0.04;
	private static final float TRANSFER_COEFFICIENT = 0.25f;

	public static final String CHARM_DURATION = "Wind Bomb Duration";
	public static final String CHARM_WEAKNESS = "Wind Bomb Weaken Amplifier";
	public static final String CHARM_SLOWNESS = "Wind Bomb Slow Amplifier";
	public static final String CHARM_COOLDOWN = "Wind Bomb Cooldown";
	public static final String CHARM_DAMAGE = "Wind Bomb Damage";
	public static final String CHARM_DAMAGE_MODIFIER = "Wind Bomb Damage Modifier";
	public static final String CHARM_RADIUS = "Wind Bomb Radius";
	public static final String CHARM_HEIGHT = "Wind Bomb Height";
	public static final String CHARM_PULL = "Wind Bomb Vortex Pull";
	public static final String CHARM_VORTEX_DURATION = "Wind Bomb Vortex Duration";
	public static final String CHARM_VORTEX_RADIUS = "Wind Bomb Vortex Radius";
	public static final String CHARM_VORTEX_HEIGHT = "Wind Bomb Vortex Height";
	public static final String CHARM_STUN_DURATION = "Wind Bomb Stun Duration";

	public static final AbilityInfo<WindBomb> INFO =
		new AbilityInfo<>(WindBomb.class, "Wind Bomb", WindBomb::new)
			.linkedSpell(ClassAbility.WIND_BOMB)
			.scoreboardId("WindBomb")
			.shorthandName("WB")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Throw a bomb that damages and launches mobs up in the air, weakening and slowing them.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", WindBomb::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.REQUIRE_PROJECTILE_WEAPON)))
			.displayItem(Material.TNT);

	private final List<Triple<ThrowableProjectile, Double, ItemStatManager.PlayerItemStats>> mProjectiles = new ArrayList<>();
	private final double mDamageFraction;
	private final double mRadius;
	private final int mEffectDuration;
	private final double mWeaknessPotency;
	private final double mSlownessPotency;
	private final double mVelocityMultSquared;
	private final double mMidairDamageMult;
	private final int mStunDuration;
	private final double mEnhancePullVelocity;
	private final double mEnhancePullRadius;
	private final int mEnhancePullDuration;
	private final double mEnhanceVortexHeight;
	private final WindBombCS mCosmetic;

	public WindBomb(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);

		mDamageFraction = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelTwo() ? DAMAGE_FRACTION_2 : DAMAGE_FRACTION_1);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mEffectDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		mWeaknessPotency = WEAKEN_EFFECT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKNESS);
		mSlownessPotency = SLOW_EFFECT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOWNESS);
		mVelocityMultSquared = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEIGHT, 1);
		mMidairDamageMult = MIDAIR_DAMAGE_BONUS + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_MODIFIER);
		mStunDuration = CharmManager.getDuration(mPlayer, CHARM_STUN_DURATION, STUN_DURATION);
		mEnhancePullVelocity = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_PULL, PULL_VELOCITY);
		mEnhancePullRadius = CharmManager.getRadius(mPlayer, CHARM_VORTEX_RADIUS, PULL_RADIUS);
		mEnhancePullDuration = CharmManager.getDuration(mPlayer, CHARM_VORTEX_DURATION, PULL_DURATION);
		mEnhanceVortexHeight = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_VORTEX_HEIGHT, VORTEX_HEIGHT);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new WindBombCS());
	}

	public boolean cast() {
		if (!mProjectiles.isEmpty()) {
			detonate(mProjectiles.get(0));
		} else if (isOnCooldown()) {
			return false;
		} else {
			mCosmetic.onThrow(mPlayer.getWorld(), mPlayer.getLocation());
			final ThrowableProjectile proj = AbilityUtils.spawnAbilitySnowball(mPlugin, mPlayer, mPlayer.getWorld(), VELOCITY, mCosmetic.getProjectileName(), mCosmetic.getProjectileParticle(), LocationUtils.isLocationInWater(mPlayer.getLocation()));
			double damage = ItemStatUtils.getAttributeAmount(mPlayer.getInventory().getItemInMainHand(),
				AttributeType.PROJECTILE_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND) * mDamageFraction;
			final ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
			mProjectiles.add(Triple.of(proj, damage, playerItemStats));
			putOnCooldown();
		}
		// Clear out list just in case
		mProjectiles.removeIf(triple -> triple.getLeft().isDead() || !triple.getLeft().isValid());
		return true;
	}

	@Override
	public void projectileHitEvent(final ProjectileHitEvent event, final Projectile proj) {
		Triple<ThrowableProjectile, Double, ItemStatManager.PlayerItemStats> triple = null;
		for (final Triple<ThrowableProjectile, Double, ItemStatManager.PlayerItemStats> testTriple : mProjectiles) {
			if (testTriple.getLeft() == proj) {
				triple = testTriple;
				break;
			}
		}

		if (triple != null) {
			event.setCancelled(true);
			detonate(triple);
		}
	}

	@Override
	public boolean onDamage(final DamageEvent event, final LivingEntity enemy) {
		final MetadataValue playerName = MetadataUtils.getMetadataValue(enemy, MIDAIR_DAMAGE_METAKEY);
		/* If the skill is L2 and the enemy hasn't been tagged by this player with the metakey and the enemy is airborne and the damage is not true */
		if (isLevelTwo() && (playerName == null || !playerName.asString().equals(mPlayer.getName())) && LocationUtils.isAirborne(enemy)
			&& event.getType() != DamageEvent.DamageType.TRUE) {
			MetadataUtils.setMetadata(enemy, MIDAIR_DAMAGE_METAKEY, new FixedMetadataValue(mPlugin, mPlayer.getName()));
			event.updateDamageWithMultiplier(1 + mMidairDamageMult);
		}
		return false;
	}

	private void applyEffects(LivingEntity mob) {
		MetadataUtils.removeMetadata(mob, MIDAIR_DAMAGE_METAKEY);
		EntityUtils.applyWeaken(mPlugin, mEffectDuration, mWeaknessPotency, mob);
		EntityUtils.applySlow(mPlugin, mEffectDuration, mSlownessPotency, mob);
		// might want to make a check if it's a boss? ideally we also apply slow fall to Twisteds for fun, but it might break stuff...
		PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW_FALLING, mEffectDuration,
			SLOW_FALL_POTENCY, true, false));
	}

	private void detonate(final Triple<ThrowableProjectile, Double, ItemStatManager.PlayerItemStats> triple) {
		if (triple != null) {
			final ThrowableProjectile proj = triple.getLeft();
			final double damage = triple.getMiddle();
			final ItemStatManager.PlayerItemStats playerItemStats = triple.getRight();
			final Location loc = proj.getLocation();

			mProjectiles.remove(triple);
			mCosmetic.onLand(mPlayer, proj.getWorld(), loc, mRadius);

			final List<LivingEntity> launchedMobs = new Hitbox.SphereHitbox(loc, mRadius).getHitMobs();
			for (final LivingEntity mob : launchedMobs) {
				applyEffects(mob);
				if (damage > 0) {
					DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.PROJECTILE_SKILL,
						mInfo.getLinkedSpell(), playerItemStats), damage, true, false, false);
				}
				if (!EntityUtils.isBoss(mob)) {
					// Velocity scales with the square root of the maximum height
					if (isEnhanced()) {
						EntityUtils.applyStun(mPlugin, mStunDuration, mob);
					} else {
						mob.setVelocity(new Vector(0, (float) (LAUNCH_VELOCITY * Math.sqrt(mVelocityMultSquared)), 0));
					}
				}
			}

			if (isEnhanced()) {
				final Location vortexLoc;
				World world = loc.getWorld();
				RayTraceResult result = world.rayTraceBlocks(loc, new Vector(0, -1, 0), mEnhanceVortexHeight, FluidCollisionMode.NEVER, true);
				if (result == null) {
					vortexLoc = loc;
				} else {
					vortexLoc = result.getHitPosition().toLocation(world).add(new Vector(0, mEnhanceVortexHeight, 0));
				}

				mCosmetic.onVortexSpawn(mPlayer, proj.getWorld(), vortexLoc, mEnhancePullDuration);

				new BukkitRunnable() {
					int mTicks = 0;

					@Override
					public void run() {
						mTicks++;
						mCosmetic.onVortexTick(mPlayer, vortexLoc, mEnhancePullRadius, mTicks);

						if (mTicks >= mEnhancePullDuration) {
							this.cancel();
							return;
						}

						if (mTicks % PULL_INTERVAL != 0) {
							return;
						}

						Hitbox.SphereHitbox vortexBox = new Hitbox.SphereHitbox(vortexLoc, mEnhancePullRadius);
						Hitbox.SphereHitbox vortexEyeBox = new Hitbox.SphereHitbox(vortexLoc, mEnhancePullRadius / 10);

						for (final LivingEntity mob : vortexBox.getHitMobs()) {
							applyEffects(mob);
							if (!EntityUtils.isCCImmuneMob(mob) || ZoneUtils.hasZoneProperty(mob.getLocation(), ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
								if (vortexEyeBox.getHitMobs().contains(mob)) {
									final Vector velocity = new Vector(0, 0.1, 0);
									mob.setVelocity(velocity);
								} else {
									final Vector vector = mob.getLocation().toVector().subtract(vortexLoc.toVector());
									final double ratio = PULL_RATIO + vector.length() / mEnhancePullRadius;
									final Vector velocity = vector.normalize().multiply(mEnhancePullVelocity)
										.multiply(-ratio);
									if (!(mob instanceof Flying)) {
										velocity.add(new Vector(0, 0.03 + 0.1 * ratio, 0));
									}
									MovementUtils.knockAwayDirection(velocity, mob, TRANSFER_COEFFICIENT);
								}
							}
						}
					}
				}.runTaskTimer(mPlugin, 0, 1);
			}
			proj.remove();
		}
	}

	private static Description<WindBomb> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to throw a projectile that, upon recast, or contact with the ground or an enemy, deals ")
			.addPercent(a -> a.mDamageFraction, DAMAGE_FRACTION_1, false, Ability::isLevelOne)
			.add(" of your projectile damage to mobs within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks and launches them into the air, giving them ")
			.addPercent(a -> a.mWeaknessPotency, WEAKEN_EFFECT)
			.add(" weakness, ")
			.addPercent(a -> a.mSlownessPotency, SLOW_EFFECT)
			.add(" slowness, and Slow Falling for ")
			.addDuration(a -> a.mEffectDuration, DURATION)
			.add(" seconds.")
			.addCooldown(COOLDOWN_1, Ability::isLevelOne);
	}

	private static Description<WindBomb> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The damage is increased to ")
			.addPercent(a -> a.mDamageFraction, DAMAGE_FRACTION_2, false, Ability::isLevelTwo)
			.add(" of your projectile damage. Additionally, a hit dealt to an airborne enemy deals ")
			.addPercent(a -> a.mMidairDamageMult, MIDAIR_DAMAGE_BONUS)
			.add(" more damage but the bonus damage can only be applied to the same enemy again if it has been hit by a Wind Bomb.")
			.addCooldown(COOLDOWN_2, Ability::isLevelTwo);
	}

	private static Description<WindBomb> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Your Wind Bomb no longer launches enemies, instead stunning them for ")
			.addDuration(a -> a.mStunDuration, STUN_DURATION)
			.add(" second. On detonation, generate a vortex that pulls mobs within ")
			.add(a -> a.mEnhancePullRadius, PULL_RADIUS)
			.add(" blocks toward the center for ")
			.addDuration(a -> a.mEnhancePullDuration, PULL_DURATION)
			.add(" seconds. The vortex applies all the effects of the unenhanced Wind Bomb, including airborne damage.");
	}
}
