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
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.util.Vector;

public class WindBomb extends Ability {
	private static final int DURATION = Constants.TICKS_PER_SECOND * 4;
	private static final double WEAKEN_EFFECT = 0.2;
	private static final double LAUNCH_VELOCITY = 1.2;
	private static final int SLOW_FALL_POTENCY = 0;
	private static final int COOLDOWN_1 = Constants.TICKS_PER_SECOND * 15;
	private static final int COOLDOWN_2 = Constants.TICKS_PER_SECOND * 10;
	private static final double DAMAGE_FRACTION_1 = 0.4;
	private static final double DAMAGE_FRACTION_2 = 0.5;
	private static final double MIDAIR_DAMAGE_BONUS = 0.2;
	private static final int RADIUS = 3;
	private static final double VELOCITY = 1.5;
	private static final String MIDAIR_DAMAGE_METAKEY = "WindBombMidairDamageBonus";

	private static final int PULL_INTERVAL = 10;
	private static final double PULL_VELOCITY = 0.35;
	private static final double PULL_RADIUS = 10;
	private static final int PULL_DURATION = Constants.TICKS_PER_SECOND * 3;
	private static final double PULL_RATIO = 0.12;

	public static final String CHARM_DURATION = "Wind Bomb Duration";
	public static final String CHARM_WEAKNESS = "Wind Bomb Weaken Amplifier";
	public static final String CHARM_COOLDOWN = "Wind Bomb Cooldown";
	public static final String CHARM_DAMAGE = "Wind Bomb Damage";
	public static final String CHARM_DAMAGE_MODIFIER = "Wind Bomb Damage Modifier";
	public static final String CHARM_RADIUS = "Wind Bomb Radius";
	public static final String CHARM_HEIGHT = "Wind Bomb Height";
	public static final String CHARM_PULL = "Wind Bomb Vortex Pull";
	public static final String CHARM_VORTEX_DURATION = "Wind Bomb Vortex Duration";
	public static final String CHARM_VORTEX_RADIUS = "Wind Bomb Vortex Radius";

	public static final AbilityInfo<WindBomb> INFO =
		new AbilityInfo<>(WindBomb.class, "Wind Bomb", WindBomb::new)
			.linkedSpell(ClassAbility.WIND_BOMB)
			.scoreboardId("WindBomb")
			.shorthandName("WB")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Throw a bomb that damages and launches mobs up in the air, weakening them.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", WindBomb::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.REQUIRE_PROJECTILE_WEAPON)))
			.displayItem(Material.TNT);

	private final List<Triple<ThrowableProjectile, Double, ItemStatManager.PlayerItemStats>> mProjectiles = new ArrayList<>();
	private final double mDamageFraction;
	private final double mRadius;
	private final int mEffectDuration;
	private final double mWeaknessPotency;
	private final double mVelocityMultSquared;
	private final double mMidairDamageMult;
	private final double mEnhancePullVelocity;
	private final double mEnhancePullRadius;
	private final int mEnhancePullDuration;
	private final WindBombCS mCosmetic;

	public WindBomb(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);

		mDamageFraction = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelTwo() ? DAMAGE_FRACTION_2 : DAMAGE_FRACTION_1);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mEffectDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		mWeaknessPotency = WEAKEN_EFFECT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKNESS);
		mVelocityMultSquared = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEIGHT, 1);
		mMidairDamageMult = MIDAIR_DAMAGE_BONUS + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_MODIFIER);
		mEnhancePullVelocity = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_PULL, PULL_VELOCITY);
		mEnhancePullRadius = CharmManager.getRadius(mPlayer, CHARM_VORTEX_RADIUS, PULL_RADIUS);
		mEnhancePullDuration = CharmManager.getDuration(mPlayer, CHARM_VORTEX_DURATION, PULL_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new WindBombCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		mCosmetic.onThrow(mPlayer.getWorld(), mPlayer.getLocation());
		final ThrowableProjectile proj = AbilityUtils.spawnAbilitySnowball(mPlugin, mPlayer, mPlayer.getWorld(), VELOCITY, mCosmetic.getProjectileName(), mCosmetic.getProjectileParticle(), LocationUtils.isLocationInWater(mPlayer.getLocation()));
		double damage = ItemStatUtils.getAttributeAmount(mPlayer.getInventory().getItemInMainHand(),
			AttributeType.PROJECTILE_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND) * mDamageFraction;
		final ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
		mProjectiles.add(Triple.of(proj, damage, playerItemStats));
		putOnCooldown();

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
			final double damage = triple.getMiddle();
			final ItemStatManager.PlayerItemStats playerItemStats = triple.getRight();
			final Location loc = proj.getLocation();

			event.setCancelled(true);
			mProjectiles.remove(triple);
			mCosmetic.onLand(mPlugin, mPlayer, proj.getWorld(), loc, mRadius);

			final List<LivingEntity> launchedMobs = new Hitbox.SphereHitbox(loc, mRadius).getHitMobs();
			for (final LivingEntity mob : launchedMobs) {
				MetadataUtils.removeMetadata(mob, MIDAIR_DAMAGE_METAKEY);
				EntityUtils.applyWeaken(mPlugin, mEffectDuration, mWeaknessPotency, mob);
				if (damage > 0) {
					DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.PROJECTILE_SKILL,
						mInfo.getLinkedSpell(), playerItemStats), damage, true, false, false);
				}
				if (!EntityUtils.isBoss(mob)) {
					// Velocity scales with the square root of the maximum height
					mob.setVelocity(new Vector(0, (float) (LAUNCH_VELOCITY * Math.sqrt(mVelocityMultSquared)), 0));
					PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW_FALLING, mEffectDuration,
						SLOW_FALL_POTENCY, true, false));
				}
			}

			if (isEnhanced()) {
				loc.add(0, 2, 0);
				mCosmetic.onVortexSpawn(mPlayer, proj.getWorld(), loc);

				new BukkitRunnable() {
					int mTicks = 0;

					@Override
					public void run() {
						mTicks++;
						mCosmetic.onVortexTick(mPlayer, loc, mEnhancePullRadius, mTicks);

						if (mTicks >= mEnhancePullDuration) {
							this.cancel();
							return;
						}

						if (mTicks % PULL_INTERVAL != 0) {
							return;
						}

						for (final LivingEntity mob : new Hitbox.SphereHitbox(loc, mEnhancePullRadius).getHitMobs()) {
							if (!EntityUtils.isCCImmuneMob(mob) || ZoneUtils.hasZoneProperty(mob.getLocation(), ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
								final Vector vector = mob.getLocation().toVector().subtract(loc.toVector());
								final double ratio = PULL_RATIO + vector.length() / mEnhancePullRadius;
								final Vector velocity = mob.getVelocity().add(vector.normalize().multiply(mEnhancePullVelocity)
									.multiply(-ratio).add(new Vector(0, 0.1 + 0.2 * ratio, 0)));
								if (launchedMobs.contains(mob)) {
									// If mob was launched by the ability, don't change their Y
									velocity.setY(mob.getVelocity().getY());
								}
								mob.setVelocity(velocity);
							}
						}
					}
				}.runTaskTimer(mPlugin, 0, 1);
			}

			proj.remove();
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

	private static Description<WindBomb> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to throw a projectile that, upon contact with the ground or an enemy, deals ")
			.addPercent(a -> a.mDamageFraction, DAMAGE_FRACTION_1, false, Ability::isLevelOne)
			.add(" of your projectile damage to mobs within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks and launches them into the air, giving them Slow Falling and ")
			.addPercent(a -> a.mWeaknessPotency, WEAKEN_EFFECT)
			.add(" weakness for ")
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
			.add("On impact, generate a vortex that pulls mobs within ")
			.add(a -> a.mEnhancePullRadius, PULL_RADIUS)
			.add(" blocks toward the center for ")
			.addDuration(a -> a.mEnhancePullDuration, PULL_DURATION)
			.add(" seconds.");
	}
}
