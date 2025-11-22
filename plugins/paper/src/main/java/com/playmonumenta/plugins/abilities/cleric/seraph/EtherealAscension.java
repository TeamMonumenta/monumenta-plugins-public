package com.playmonumenta.plugins.abilities.cleric.seraph;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.seraph.EtherealAscensionCS;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentThrowRate;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Chaotic;
import com.playmonumenta.plugins.itemstats.enchantments.Duelist;
import com.playmonumenta.plugins.itemstats.enchantments.HexEater;
import com.playmonumenta.plugins.itemstats.enchantments.PointBlank;
import com.playmonumenta.plugins.itemstats.enchantments.Slayer;
import com.playmonumenta.plugins.itemstats.enchantments.Smite;
import com.playmonumenta.plugins.itemstats.enchantments.Sniper;
import com.playmonumenta.plugins.itemstats.enchantments.ThrowingKnife;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class EtherealAscension extends Ability implements AbilityWithDuration {

	private static final double ASCENSION_ORB_DAMAGE_FLAT = 4;
	private static final double ASCENSION_ORB_DAMAGE_PERCENT = 0.85;
	private static final double ASCENSION_ORB_RADIUS = 2;
	private static final double ASCENSION_ORB_TRAVEL_SPEED = 1.7;
	private static final double ASCENSION_ORB_DAMAGE_BONUS = 0.3;
	private static final int ASCENSION_ORB_HASTE = 1; // Actually 2 because of how effects work
	private static final int ASCENSION_ORB_BUFF_DURATION = 8 * 20;
	private static final double ASCENSION_THROW_RATE = 0.1;
	private static final double ASCENSION_ORB_RADIUS_PERCENT_BONUS = 0.15;
	private static final int ASCENSION_DURATION_EXTENSION = 10;
	private static final int ASCENSION_DURATION_MAX_EXTENSION = 4 * 20;
	private static final double ASCENSION_LAUNCH_KNOCKBACK_RADIUS = 5;
	private static final double ASCENSION_HOVER_HEIGHT = 3.5;
	private static final float ASCENSION_LAUNCH_VELOCITY = 0.7f;
	private static final float ASCENSION_DASH_VELOCITY = 0.8f;
	private static final int ASCENSION_DURATION = 12 * 20;
	private static final int ASCENSION_COOLDOWN = 25 * 20;

	public static final String CHARM_DAMAGE = "Ethereal Ascension Orb Damage";
	public static final String CHARM_RANGE = "Ethereal Ascension Orb Max Range";
	public static final String CHARM_RADIUS = "Ethereal Ascension Orb Radius";
	public static final String CHARM_TRAVEL_SPEED = "Ethereal Ascension Orb Travel Speed";
	public static final String CHARM_KNOCKBACK = "Ethereal Ascension Orb Knockback";
	public static final String CHARM_DAMAGE_BONUS = "Ethereal Ascension Orb Damage Amplifier";
	public static final String CHARM_HASTE = "Ethereal Ascension Orb Haste Amplifier";
	public static final String CHARM_BUFF_DURATION = "Ethereal Ascension Orb Buff Duration";
	public static final String CHARM_THROW_RATE = "Ethereal Ascension Throw Rate Amplifier";
	public static final String CHARM_DURATION_EXTENSION = "Ethereal Ascension Duration Extension";
	public static final String CHARM_DURATION_MAX_EXTENSION = "Ethereal Ascension Max Duration Extension";
	public static final String CHARM_LAUNCH_KNOCKBACK = "Ethereal Ascension Launch Knockback";
	public static final String CHARM_LAUNCH_KNOCKBACK_RADIUS = "Ethereal Ascension Launch Knockback Radius";
	public static final String CHARM_HOVER_HEIGHT = "Ethereal Ascension Hover Height";
	public static final String CHARM_DASH_VELOCITY = "Ethereal Ascension Dash Velocity";
	public static final String CHARM_DURATION = "Ethereal Ascension Duration";
	public static final String CHARM_COOLDOWN = "Ethereal Ascension Cooldown";

	public static final String NOT_FULLY_CHARGED_MARKER = "Non-critical Ethereal Ascension Orb";

	public static final AbilityInfo<EtherealAscension> INFO =
		new AbilityInfo<>(EtherealAscension.class, "Ethereal Ascension", EtherealAscension::new)
			.linkedSpell(ClassAbility.ETHEREAL_ASCENSION)
			.scoreboardId("EtherealAscension")
			.shorthandName("Asc")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Take flight temporarily and convert your projectiles into magical orbs that damage mobs and buff players.")
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", EtherealAscension::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)))
			.cooldown(ASCENSION_COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.TOTEM_OF_UNDYING);

	private final double mAscensionOrbDamageFlat;
	private final double mAscensionOrbDamagePercent;
	private final double mAscensionOrbRadius;
	private final double mAscensionOrbTravelSpeed;
	private final float mAscensionOrbKnockback;
	private final double mAscensionOrbDamageBonus;
	private final int mAscensionOrbHaste;
	private final int mAscensionOrbBuffDuration;
	private final double mAscensionThrowRate;
	private final int mAscensionDurationExtension;
	private final int mAscensionMaxDurationExtension;
	private final float mAscensionLaunchKnockback;
	private final double mAscensionLaunchKnockbackRadius;
	private final double mAscensionHoverHeight;
	private final float mAscensionLaunchVelocity;
	private final float mAscensionDashVelocity;
	private final int mAscensionDuration;
	private final EtherealAscensionCS mCosmetic;

	private @Nullable BukkitRunnable mAscendRunnable;
	private int mCurrentDuration = -1;
	private int mDurationExtension = 0;
	private int mMultishotCounter = 0;

	public EtherealAscension(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mAscensionOrbDamageFlat = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, ASCENSION_ORB_DAMAGE_FLAT);
		mAscensionOrbDamagePercent = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, ASCENSION_ORB_DAMAGE_PERCENT);
		mAscensionOrbRadius = CharmManager.getRadius(player, CHARM_RADIUS, ASCENSION_ORB_RADIUS);
		mAscensionOrbTravelSpeed = CharmManager.calculateFlatAndPercentValue(player, CHARM_TRAVEL_SPEED, ASCENSION_ORB_TRAVEL_SPEED);
		mAscensionOrbKnockback = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_KNOCKBACK, 1);
		mAscensionOrbDamageBonus = ASCENSION_ORB_DAMAGE_BONUS + CharmManager.getLevelPercentDecimal(player, CHARM_DAMAGE_BONUS);
		mAscensionOrbHaste = ASCENSION_ORB_HASTE + (int) CharmManager.getLevel(player, CHARM_HASTE);
		mAscensionOrbBuffDuration = CharmManager.getDuration(player, CHARM_BUFF_DURATION, ASCENSION_ORB_BUFF_DURATION);
		mAscensionThrowRate = ASCENSION_THROW_RATE + CharmManager.getLevelPercentDecimal(player, CHARM_THROW_RATE);
		mAscensionDurationExtension = CharmManager.getDuration(player, CHARM_DURATION_EXTENSION, ASCENSION_DURATION_EXTENSION);
		mAscensionMaxDurationExtension = CharmManager.getDuration(player, CHARM_DURATION_MAX_EXTENSION, ASCENSION_DURATION_MAX_EXTENSION);
		mAscensionLaunchKnockback = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_LAUNCH_KNOCKBACK, 1);
		mAscensionLaunchKnockbackRadius = CharmManager.getRadius(player, CHARM_LAUNCH_KNOCKBACK_RADIUS, ASCENSION_LAUNCH_KNOCKBACK_RADIUS);
		mAscensionHoverHeight = CharmManager.calculateFlatAndPercentValue(player, CHARM_HOVER_HEIGHT, ASCENSION_HOVER_HEIGHT);
		mAscensionLaunchVelocity = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_HOVER_HEIGHT, ASCENSION_LAUNCH_VELOCITY);
		mAscensionDashVelocity = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_DASH_VELOCITY, ASCENSION_DASH_VELOCITY);
		mAscensionDuration = CharmManager.getDuration(player, CHARM_DURATION, ASCENSION_DURATION);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new EtherealAscensionCS());
	}

	public boolean cast() {
		if (isOnCooldown() || mCurrentDuration >= 0 || ZoneUtils.hasZoneProperty(mPlayer.getLocation(), ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
			if (mCurrentDuration >= 20) {
				// Cancel if active for more than a second
				if (mAscendRunnable != null) {
					putOnCooldown();
					mCosmetic.forceEndAscension(mPlayer, mPlayer.getWorld(), mPlayer.getLocation());
					mAscendRunnable.cancel();
				}
			}
			return false;
		}

		if (mAscendRunnable != null) {
			mAscendRunnable.cancel();
		}
		EntityUtils.getNearbyMobs(mPlayer.getLocation(), mAscensionLaunchKnockbackRadius).forEach(m -> MovementUtils.knockAway(mPlayer.getLocation(), m, 0.6f * mAscensionLaunchKnockback, 0.3f * mAscensionLaunchKnockback));
		mPlayer.setVelocity(new Vector(0, mAscensionLaunchVelocity, 0));
		mCosmetic.onLaunch(mPlayer, mPlayer.getWorld(), mPlayer.getLocation());
		mPlugin.mPotionManager.addPotion(mPlayer, PotionManager.PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SLOW_FALLING, mAscensionDuration, 0, false, false));

		mDurationExtension = 0;
		mCurrentDuration = 1;
		ClientModHandler.updateAbility(mPlayer, this);
		mPlayer.setFlySpeed(0);
		mAscendRunnable = new BukkitRunnable() {
			int mLastDashTick = 0;

			@Override
			public void run() {
				if (!mPlayer.isOnline() || mPlayer.isDead() || AbilityUtils.isSilenced(mPlayer) || ZoneUtils.hasZoneProperty(mPlayer.getLocation(), ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
					putOnCooldown();
					this.cancel();
					return;
				}
				Location loc = mPlayer.getLocation();
				Location eyeLoc = mPlayer.getEyeLocation();
				double distance = LocationUtils.distanceToGround(loc, -64);
				boolean belowCeiling = false;
				boolean almostBelowCeiling = false;
				for (int i = 0; i < 6; i++) {
					eyeLoc.add(0, 0.5, 0);
					if (!eyeLoc.getBlock().isEmpty() && i < 4) {
						belowCeiling = true;
					} else if (!eyeLoc.getBlock().isEmpty()) {
						almostBelowCeiling = true;
					}
				}
				if (distance < mAscensionHoverHeight && !mPlayer.isSneaking() && !belowCeiling) {
					int amplifier = 0;
					if (distance < 0.5 * mAscensionHoverHeight && !almostBelowCeiling) {
						amplifier = 2;
					}
					mPlugin.mPotionManager.addPotion(mPlayer, PotionManager.PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.LEVITATION, 9, amplifier, false, false));
				}
				if (isLevelTwo()) {
					mPlugin.mEffectManager.addEffect(mPlayer, "EtherealAscensionThrowRate", new PercentThrowRate(10, mAscensionThrowRate).displaysTime(false));
				}
				if (Bukkit.getCurrentTick() - mLastDashTick >= 40) {
					mPlayer.setAllowFlight(true);
				}
				if (mPlayer.isFlying()) {
					mLastDashTick = Bukkit.getCurrentTick();
					mPlayer.setAllowFlight(false);
					mPlayer.setFlying(false);
					mCosmetic.dash(mPlayer, mPlayer.getWorld(), mPlayer.getLocation());
					Vector dir = mPlayer.getLocation().getDirection().setY(0).normalize();
					mPlayer.setVelocity(dir.setY(0.35).multiply(mAscensionDashVelocity));
				}
				if (mCurrentDuration >= 0) {
					mCosmetic.tickEffect(mPlayer, mPlayer.getLocation(), mAscensionHoverHeight);
					mCurrentDuration++;
				}
				if (mCurrentDuration > getInitialAbilityDuration()) {
					putOnCooldown();
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				mCurrentDuration = -1;
				mPlugin.mEffectManager.clearEffects(mPlayer, "EtherealAscensionThrowRate");
				mPlugin.mPotionManager.clearPotionEffectType(mPlayer, PotionEffectType.SLOW_FALLING);
				mPlugin.mPotionManager.clearPotionEffectType(mPlayer, PotionEffectType.LEVITATION);
				mCosmetic.ascensionEnd(mPlayer, mPlayer.getWorld(), mPlayer.getLocation());
				ClientModHandler.updateAbility(mPlayer, EtherealAscension.this);
				if (mPlayer.getGameMode() == GameMode.SPECTATOR || mPlayer.getGameMode() == GameMode.CREATIVE) {
					mPlayer.setAllowFlight(true);
					mPlayer.setFlySpeed(0.1f);
				} else {
					mPlayer.setAllowFlight(false);
					mPlayer.setFlying(false);
				}
			}
		};
		cancelOnDeath(mAscendRunnable.runTaskTimer(mPlugin, 0, 1));
		return true;
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		ItemStatManager.PlayerItemStats playerItemStats = DamageListener.getProjectileItemStats(projectile);
		if (mCurrentDuration < 0 || playerItemStats == null) {
			return true;
		}
		ItemStatManager.PlayerItemStats.ItemStatsMap itemStatsMap = playerItemStats.getItemStats();
		if (itemStatsMap.get(EnchantmentType.GRAPPLING) > 0 || itemStatsMap.get(AttributeType.PROJECTILE_DAMAGE_ADD) <= 0) {
			return true;
		}

		double damageMultiplier;
		boolean isArrow = (projectile instanceof Arrow || projectile instanceof SpectralArrow) && !ThrowingKnife.isThrowingKnife((AbstractArrow) projectile);
		if (isArrow && !((AbstractArrow) projectile).isShotFromCrossbow()) {
			damageMultiplier = PlayerUtils.calculateBowDraw((AbstractArrow) projectile);
		} else {
			damageMultiplier = 1;
		}
		if (damageMultiplier < 0.3) {
			return true;
		}
		World world = projectile.getWorld();
		Vector dir;
		if (mMultishotCounter > 0) {
			dir = NmsUtils.getVersionAdapter().getActualDirection(mPlayer);
			final Location l = mPlayer.getLocation();
			l.setPitch(l.getPitch() - 90);
			dir.rotateAroundNonUnitAxis(l.getDirection(), (mMultishotCounter == 1 ? -1 : 1) * 10.0 * Math.PI / 180);
		} else {
			dir = NmsUtils.getVersionAdapter().getActualDirection(mPlayer);
		}
		projectile.remove();

		if (mMultishotCounter == 0) {
			Bukkit.getScheduler().runTask(mPlugin, () -> mMultishotCounter = 0);
		}
		Location startLoc = mPlayer.getEyeLocation();
		if (mMultishotCounter == 0) {
			mCosmetic.orbShoot(mPlayer, world, startLoc);
		}
		mMultishotCounter++;

		cancelOnDeath(new BukkitRunnable() {
			final Location mLoc = startLoc.clone();
			final Vector mIncrement = dir.multiply(mAscensionOrbTravelSpeed);

			@Override
			public void run() {
				for (int i = 0; i < 5; i++) {
					mLoc.add(mIncrement.clone().multiply(0.25));
					mCosmetic.orbTrail(mPlayer, mLoc, startLoc);
					final Hitbox impactHitbox = new Hitbox.SphereHitbox(mLoc, 0.5);
					final Hitbox playerHitbox = new Hitbox.SphereHitbox(mLoc, 0.75);
					if (!impactHitbox.getHitMobs().isEmpty() || !playerHitbox.getHitPlayers(mPlayer, true).isEmpty() || !mLoc.isChunkLoaded() ||
						LocationUtils.collidesWithBlocks(BoundingBox.of(mLoc.clone().add(0.5 / 2, 0.5 / 2, 0.5 / 2),
								mLoc.clone().add(-0.5 / 2, -0.5 / 2, -0.5 / 2)),
							mLoc.getWorld(), false) || mLoc.distance(startLoc) > 30) {

						double radius = mAscensionOrbRadius * (isArrow && isLevelTwo() && damageMultiplier == 1 ? 1 + ASCENSION_ORB_RADIUS_PERCENT_BONUS : 1);
						Hitbox aoeHitbox = new Hitbox.SphereHitbox(mLoc, radius);
						aoeHitbox.getHitMobs().forEach(mob -> {
							if (MetadataUtils.checkOnceInRecentTicks(mPlugin, mob, "EtherealAscensionHit", 8)) {
								double damage = itemStatsMap.get(AttributeType.PROJECTILE_DAMAGE_ADD);
								// Apply all base damage enchants
								damage += Sniper.apply(mPlayer, mob, itemStatsMap.get(EnchantmentType.SNIPER));
								damage += PointBlank.apply(mPlayer, mob, itemStatsMap.get(EnchantmentType.POINT_BLANK));
								damage += HexEater.calculateHexDamage(mPlugin, true, mPlayer, (int) itemStatsMap.get(EnchantmentType.HEX_EATER), mob);
								damage += Smite.calculateSmiteDamage(true, mPlayer, itemStatsMap.get(EnchantmentType.SMITE), mob);
								damage += Slayer.calculateSlayerDamage(true, mPlayer, itemStatsMap.get(EnchantmentType.SLAYER), mob);
								damage += Duelist.calculateDuelistDamage(true, mPlayer, itemStatsMap.get(EnchantmentType.DUELIST), mob);
								damage += Chaotic.calculateChaoticDamage(true, mPlayer, itemStatsMap.get(EnchantmentType.CHAOTIC), mob);
								damage *= itemStatsMap.get(AttributeType.PROJECTILE_DAMAGE_MULTIPLY);

								if (damageMultiplier == 1) {
									// "Critical" Ethereal Ascension orb, transfers aspects
									DamageUtils.damage(mPlayer, mob,
										new DamageEvent.Metadata(
											DamageEvent.DamageType.MAGIC,
											mInfo.getLinkedSpell(),
											playerItemStats),
										mAscensionOrbDamageFlat + mAscensionOrbDamagePercent * damage,
										true, false, false);
								} else {
									// "Non-critical" Ethereal Ascension orb, does not transfer aspects
									DamageUtils.damage(mPlayer, mob,
										new DamageEvent.Metadata(
											DamageEvent.DamageType.MAGIC,
											mInfo.getLinkedSpell(),
											playerItemStats,
											NOT_FULLY_CHARGED_MARKER),
										damageMultiplier * (mAscensionOrbDamageFlat + mAscensionOrbDamagePercent * damage),
										true, false, false);
								}
								MovementUtils.knockAway(mLoc, mob, 0.2f * mAscensionOrbKnockback, 0.2f * mAscensionOrbKnockback, true);
							}
						});
						aoeHitbox.getHitPlayers(mPlayer, true).forEach(p -> {
							mPlugin.mEffectManager.addEffect(p, "EtherealAscensionDamage", new PercentDamageDealt(mAscensionOrbBuffDuration, mAscensionOrbDamageBonus));
							mPlugin.mPotionManager.addPotion(p, PotionManager.PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.FAST_DIGGING, mAscensionOrbBuffDuration, mAscensionOrbHaste, true, true, true));
						});
						if (!aoeHitbox.getHitPlayers(mPlayer, true).isEmpty()) {
							mCosmetic.orbBuff(mPlayer, world, mLoc, aoeHitbox.getHitPlayers(mPlayer, true), radius);
						}
						mCosmetic.orbImpact(mPlayer, world, mLoc, radius);
						this.cancel();
						break;
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));

		return true;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (isLevelTwo() && mCurrentDuration >= 0 && mDurationExtension < mAscensionMaxDurationExtension) {
			mDurationExtension += mAscensionDurationExtension;
			mPlugin.mPotionManager.addPotion(mPlayer, PotionManager.PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SLOW_FALLING, getRemainingAbilityDuration(), 0, false, false));
			ClientModHandler.updateAbility(mPlayer, ClassAbility.ETHEREAL_ASCENSION);
		}
	}

	private static Description<EtherealAscension> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to Ascend and hover above the ground for ")
			.addDuration(a -> a.mAscensionDuration, ASCENSION_DURATION)
			.add("s. While Ascended, projectiles you shoot turn into orbs that move in a straight line, applying aspect enchants and dealing up to ")
			.add(a -> a.mAscensionOrbDamageFlat, ASCENSION_ORB_DAMAGE_FLAT)
			.add(" + ")
			.addPercent(a -> a.mAscensionOrbDamagePercent, ASCENSION_ORB_DAMAGE_PERCENT)
			.add(" of your base projectile damage when fully charged, including flat damage buffs, as magic damage to mobs in a ")
			.add(a -> a.mAscensionOrbRadius, ASCENSION_ORB_RADIUS)
			.add(" block radius on impact. A mob can only be hit by one orb every 0.4s. Hitting allies with an orb grants them a ")
			.addPercent(a -> a.mAscensionOrbDamageBonus, ASCENSION_ORB_DAMAGE_BONUS)
			.add(" damage bonus and Haste ")
			.addPotionAmplifier(a -> a.mAscensionOrbHaste, ASCENSION_ORB_HASTE)
			.add(" for ")
			.addDuration(a -> a.mAscensionOrbBuffDuration, ASCENSION_ORB_BUFF_DURATION)
			.add("s. Double-jumping will cause you to dash forward, at most once every 2s. Ascension ends and goes on cooldown when the duration expires or you recast the ability.")
			.addCooldown(ASCENSION_COOLDOWN);
	}

	private static Description<EtherealAscension> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("You gain ")
			.addPercent(ASCENSION_ORB_RADIUS_PERCENT_BONUS)
			.add(" increased orb radius from fully charged bow and crossbow shots, and ")
			.addPercent(a -> a.mAscensionThrowRate, ASCENSION_THROW_RATE)
			.add(" faster throw rate while Ascended. For each mob you kill while Ascended, extend the duration by ")
			.addDuration(a -> a.mAscensionDurationExtension, ASCENSION_DURATION_EXTENSION)
			.add("s, up to a total of ")
			.addDuration(a -> a.mAscensionMaxDurationExtension, ASCENSION_DURATION_MAX_EXTENSION)
			.add(" extra seconds.");
	}

	@Override
	public void invalidate() {
		if (mAscendRunnable != null && !mAscendRunnable.isCancelled()) {
			mAscendRunnable.cancel();
			putOnCooldown();
		}
	}

	@Override
	public int getInitialAbilityDuration() {
		return mAscensionDuration + mDurationExtension;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return this.mCurrentDuration >= 0 ? getInitialAbilityDuration() - this.mCurrentDuration : 0;
	}
}
