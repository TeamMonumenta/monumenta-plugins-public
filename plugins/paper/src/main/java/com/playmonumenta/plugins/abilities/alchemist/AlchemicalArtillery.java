package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.EsotericEnhancements;
import com.playmonumenta.plugins.classes.Alchemist;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.AlchemicalArtilleryCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;

public class AlchemicalArtillery extends Ability {
	private static final int COOLDOWN = 20 * 6;
	private static final double DAMAGE_MULTIPLIER_1 = 1.15;
	private static final double DAMAGE_MULTIPLIER_2 = 1.30;
	private static final double DAMAGE_RAW_1 = 5;
	private static final double DAMAGE_RAW_2 = 6;
	private static final double RADIUS_MULTIPLIER_1 = 1.5;
	private static final double RADIUS_MULTIPLIER_2 = 1.75;
	private static final double VELOCITY_MULTIPLIER = 1;
	private static final int POTION_COST = 2;
	private static final int PROJECTILE_SIZE = 2;
	private static final int ENHANCEMENT_MAX_BOUNCES = 2;
	private static final double ENHANCEMENT_ADDED_DAMAGE_MULTIPLIER_PER_BOUNCE = 0.8;
	private static final double ENHANCEMENT_ADDED_DAMAGE_RAW_PER_BOUNCE = 2;
	private static final double ENHANCEMENT_BOUNCE_DAMAGE_FRACTION = 0.5;

	public static final String CHARM_COOLDOWN = "Alchemical Artillery Cooldown";
	public static final String CHARM_DAMAGE = "Alchemical Artillery Damage";
	public static final String CHARM_RADIUS = "Alchemical Artillery Radius";
	public static final String CHARM_VELOCITY = "Alchemical Artillery Velocity";
	public static final String CHARM_SIZE = "Alchemical Artillery Size";
	public static final String CHARM_BOUNCE_COUNT = "Alchemical Artillery Bounce Count";
	public static final String CHARM_BOUNCE_DAMAGE_MULTIPLIER_INCREASE = "Alchemical Artillery Bounce Damage Percent";
	public static final String CHARM_BOUNCE_DAMAGE_RAW_INCREASE = "Alchemical Artillery Bounce Damage Raw";
	public static final String CHARM_BOUNCE_DAMAGE_FRACTION = "Alchemical Artillery Bounce Damage Fraction";
	public static final String CHARM_COST = "Alchemical Artillery Potion Cost";

	public static final AbilityInfo<AlchemicalArtillery> INFO =
		new AbilityInfo<>(AlchemicalArtillery.class, "Alchemical Artillery", AlchemicalArtillery::new)
			.linkedSpell(ClassAbility.ALCHEMICAL_ARTILLERY)
			.scoreboardId("Alchemical")
			.shorthandName("AA")
			.actionBarColor(TextColor.color(255, 0, 0))
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Launch a bomb in the direction you're looking, which damages mobs on impact and applies your Brutal potion's effects.")
			.quest216Message("-------o-------b-------")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", AlchemicalArtillery::cast, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(false),
				PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.displayItem(Material.CROSSBOW);

	private final double mBaseVelocityMult;
	private final double mRadiusMult;
	private final double mDamageMult;
	private final double mDamageRaw;
	private final int mMaxBounceCount;
	private final double mBounceDamageMultiplierIncrease;
	private final double mBounceDamageRawIncrease;
	private final double mBounceDamageFraction;
	private final int mCost;
	private final int mSize;

	private @Nullable AlchemistPotions mAlchemistPotions;
	private @Nullable GruesomeAlchemy mGruesomeAlchemy;
	private @Nullable BrutalAlchemy mBrutalAlchemy;
	private @Nullable EsotericEnhancements mEsotericEnhancements;

	private final AlchemicalArtilleryCS mCosmetic;

	public AlchemicalArtillery(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mBaseVelocityMult = VELOCITY_MULTIPLIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_VELOCITY);
		mRadiusMult = CharmManager.getRadius(mPlayer, CHARM_RADIUS, isLevelOne() ? RADIUS_MULTIPLIER_1 : RADIUS_MULTIPLIER_2);
		mDamageMult = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_MULTIPLIER_1 : DAMAGE_MULTIPLIER_2);
		mDamageRaw = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_RAW_1 : DAMAGE_RAW_2);
		mMaxBounceCount = Math.max(0, ENHANCEMENT_MAX_BOUNCES + (int) CharmManager.getLevel(mPlayer, CHARM_BOUNCE_COUNT));
		mBounceDamageMultiplierIncrease = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_BOUNCE_DAMAGE_MULTIPLIER_INCREASE, ENHANCEMENT_ADDED_DAMAGE_MULTIPLIER_PER_BOUNCE);
		mBounceDamageRawIncrease = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_BOUNCE_DAMAGE_RAW_INCREASE, ENHANCEMENT_ADDED_DAMAGE_RAW_PER_BOUNCE);
		mBounceDamageFraction = ENHANCEMENT_BOUNCE_DAMAGE_FRACTION + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_BOUNCE_DAMAGE_FRACTION);
		mCost = POTION_COST + (int) CharmManager.getLevel(mPlayer, CHARM_COST);
		mSize = PROJECTILE_SIZE + (int) CharmManager.getLevel(mPlayer, CHARM_SIZE);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new AlchemicalArtilleryCS());

		Bukkit.getScheduler().runTask(
			plugin,
			() -> {
				mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
				mGruesomeAlchemy = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, GruesomeAlchemy.class);
				mBrutalAlchemy = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, BrutalAlchemy.class);
				mEsotericEnhancements = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, EsotericEnhancements.class);
			}
		);
	}

	public boolean cast() {
		if (mAlchemistPotions == null || isOnCooldown()) {
			return false;
		}

		// Cast new grenade
		if (mAlchemistPotions.decrementCharges(mCost)) {
			Location loc = mPlayer.getEyeLocation();
			spawnGrenade(loc);
			putOnCooldown();
			return true;
		}
		return false;
	}

	private void spawnGrenade(Location loc) {
		if (mAlchemistPotions == null) {
			return;
		}

		mCosmetic.onSpawn(loc.getWorld(), loc);
		double basePotionVelocity = (mAlchemistPotions.getSpeed() - 1) / 2 + 1;
		double velocityMultiplier = basePotionVelocity * mBaseVelocityMult;
		Vector vel = loc.getDirection().normalize().multiply(velocityMultiplier);

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		Entity e = LibraryOfSoulsIntegration.summon(loc, "AlchemicalGrenade");
		if (e instanceof MagmaCube grenade) {
			grenade.setSize(mSize);

			// Adjust the Y velocity to make the arc easier to calculate and use
			double velY = vel.getY();
			if (velY > 0 && velY < 0.2) {
				vel.setY(0.2);
			}

			// Mount the grenade on a dropped item to use its physics instead
			Item physicsItem = (Item) loc.getWorld().spawnEntity(loc, EntityType.DROPPED_ITEM);
			ItemStack itemStack = new ItemStack(Material.GUNPOWDER);
			itemStack.setAmount(1);
			physicsItem.setItemStack(itemStack);
			physicsItem.setCanPlayerPickup(false);
			physicsItem.setCanMobPickup(false);
			physicsItem.setVelocity(vel);
			physicsItem.addPassenger(grenade);
			EntityUtils.makeItemInvulnerable(physicsItem);

			new BukkitRunnable() {
				int mTicks = 0;
				int mBounceCount = 0;
				Vector mCurrVelocity = vel.clone();
				final boolean mIsEnhanced = isEnhanced();
				final ItemStatManager.PlayerItemStats mPlayerItemStats = playerItemStats;
				private final MagmaCube mGrenade = grenade;
				private final Item mPhysicsItem = physicsItem;

				private @Nullable Vector rayTraceVelForBounceVector(Vector vel) {
					double minLength = 0.15;
					double y = vel.getY();
					double lengthY = Math.max(minLength * 2, Math.abs(vel.getY()));
					// For the downwards part, the item could also be stopped dead on the ground, and needs to bounce.
					if (y > 0 && rayTraceFace(new Vector(0, 1, 0), lengthY)) {
						return new Vector(1, -1, 1);
					} else if ((mPhysicsItem.isOnGround() || y < 0) && rayTraceFace(new Vector(0, -1, 0), lengthY)) {
						return new Vector(1, -1, 1);
					}
					double x = vel.getX();
					double lengthX = Math.max(minLength, Math.abs(vel.getX()));
					// x = 0 should check both cases to prevent the item being stuck after stopping due to jank.
					if (x >= 0 && rayTraceFace(new Vector(1, 0, 0), lengthX)) {
						return new Vector(-1, 1, 1);
					} else if (x <= 0 && rayTraceFace(new Vector(-1, 0, 0), lengthX)) {
						return new Vector(-1, 1, 1);
					}
					double z = vel.getZ();
					double lengthZ = Math.max(minLength, Math.abs(vel.getZ()));
					// z = 0 should do the same as above.
					if (z >= 0 && rayTraceFace(new Vector(0, 0, 1), lengthZ)) {
						return new Vector(1, 1, -1);
					} else if (z <= 0 && rayTraceFace(new Vector(0, 0, -1), lengthZ)) {
						return new Vector(1, 1, -1);
					}
					return null;
				}

				private boolean rayTraceFace(Vector faceDir, double length) {
					Location entityCenter = LocationUtils.getHalfHeightLocation(mPhysicsItem);
					double halfWidth = mPhysicsItem.getWidth() / 2;
					double[] rotation = VectorUtils.vectorToRotation(faceDir);
					Vector up = VectorUtils.rotationToVector(rotation[0], rotation[1] - 90).normalize();
					Vector left = VectorUtils.crossProd(faceDir, up).normalize();
					up.multiply(halfWidth);
					left.multiply(halfWidth);
					Location upLeft = entityCenter.clone().add(faceDir.clone().multiply(halfWidth))
						.add(up).add(left);
					if (rayTraceHit(faceDir, upLeft, length)) {
						return true;
					}
					Location upRight = entityCenter.clone().add(faceDir.clone().multiply(halfWidth))
						.add(up).subtract(left);
					if (rayTraceHit(faceDir, upRight, length)) {
						return true;
					}
					Location downLeft = entityCenter.clone().add(faceDir.clone().multiply(halfWidth))
						.subtract(up).add(left);
					if (rayTraceHit(faceDir, downLeft, length)) {
						return true;
					}
					Location downRight = entityCenter.clone().add(faceDir.clone().multiply(halfWidth))
						.subtract(up).subtract(left);
					return rayTraceHit(faceDir, downRight, length);
				}

				private boolean rayTraceHit(Vector dir, Location from, double length) {
					RayTraceResult rayTraceResult = mGrenade.getWorld().rayTraceBlocks(
						from,
						dir.clone(),
						length,
						FluidCollisionMode.NEVER,
						true
					);
					if (rayTraceResult != null && rayTraceResult.getHitBlockFace() != null) {
						Vector hitBlockFaceDir = rayTraceResult.getHitBlockFace().getDirection();
						Location pos = LocationUtils.getHalfHeightLocation(mPhysicsItem)
							.add(hitBlockFaceDir.clone().multiply(-mPhysicsItem.getWidth() / 2));
						mCosmetic.bounceEffect(mPlayer, pos, mBounceCount,
							rayTraceResult.getHitPosition(), hitBlockFaceDir);
						return true;
					}
					return false;
				}

				@Override
				public void run() {
					if (!mPlayer.isOnline()) {
						mGrenade.remove();
						mPhysicsItem.remove();
						this.cancel();
						return;
					}

					if (mIsEnhanced) {
						Vector newVel = mPhysicsItem.getVelocity().clone();
						boolean bouncesHappened = false;
						Vector bounceVector = rayTraceVelForBounceVector(newVel);
						while (bounceVector != null) {
							mBounceCount++;
							bouncesHappened = true;
							if (mPhysicsItem.isOnGround()) {
								NmsUtils.getVersionAdapter().setNotOnGround(mPhysicsItem);
								// Use the previous velocity, as the new one is dead on the ground.
								mPhysicsItem.setVelocity(mCurrVelocity.multiply(bounceVector));
							} else if (newVel.getX() == 0) {
								// The item got stuck clipping the lower half of its hitbox on a block
								// on the X axis, before the server could register the raycast.
								mPhysicsItem.setVelocity(mCurrVelocity.multiply(bounceVector));
								EntityUtils.teleportStack(mPhysicsItem, mPhysicsItem.getLocation().add(Math.signum(mCurrVelocity.getX()) * 0.2, 0, 0));
							} else if (newVel.getZ() == 0) {
								// Same as above, but for the Z axis.
								mPhysicsItem.setVelocity(mCurrVelocity.multiply(bounceVector));
								EntityUtils.teleportStack(mPhysicsItem, mPhysicsItem.getLocation().add(0, 0, Math.signum(mCurrVelocity.getZ()) * 0.2));
							} else {
								newVel.multiply(bounceVector);
								mCurrVelocity = newVel;
								mPhysicsItem.setVelocity(newVel);
							}
							if (mBounceCount >= mMaxBounceCount) {
								break;
							}
							bounceVector = rayTraceVelForBounceVector(newVel);
						}

						if (bouncesHappened) {
							// Reset the expiry timer to allow charms with a lot of bounces to work
							mTicks = 0;
						} else {
							mCurrVelocity = newVel;
						}
					}

					// Explosion conditions
					// - If the grenade somehow died
					// - If the physics item hit the ground
					// - If the grenade hit lava
					// - If the grenade has collided with any enemy
					// - If 6 seconds have passed (probably stuck in webs)
					if (
						!mGrenade.isValid() ||
						mTicks > 120 ||
						mBounceCount > mMaxBounceCount ||
						(!mIsEnhanced && mPhysicsItem.isOnGround()) ||
						mGrenade.isInLava() ||
						hasCollidedWithEnemy(mGrenade, mPhysicsItem)
					) {
						int actualBounceCount = Math.min(mBounceCount, mMaxBounceCount);
						explode(mGrenade.getLocation().subtract(0, 0.1, 0), mPlayerItemStats, actualBounceCount);
						mGrenade.remove();
						mPhysicsItem.remove();
						this.cancel();
						return;
					}

					mCosmetic.periodicEffects(mPlayer, mGrenade, mPhysicsItem, mTicks);

					mTicks++;
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	private boolean hasCollidedWithEnemy(MagmaCube grenade, Item physicsItem) {
		Hitbox grenadeHitbox = new Hitbox.AABBHitbox(grenade.getWorld(), grenade.getBoundingBox());
		if (!grenadeHitbox.getHitMobs().isEmpty()) {
			return true;
		}
		Hitbox itemHitbox = new Hitbox.AABBHitbox(physicsItem.getWorld(), physicsItem.getBoundingBox());
		return !itemHitbox.getHitMobs().isEmpty();
	}

	private void explode(Location loc, ItemStatManager.PlayerItemStats playerItemStats, int bounceCount) {
		if (!mPlayer.isOnline() || mAlchemistPotions == null) {
			return;
		}

		double damageMultiplier = mDamageMult;
		double damageRaw = mDamageRaw;
		if (isEnhanced()) {
			double bounceDamageRaw = 0;
			double bounceDamageMult = 0;
			for (int i = 0; i < bounceCount; i++) {
				bounceDamageRaw += mBounceDamageRawIncrease * Math.pow(mBounceDamageFraction, i);
				bounceDamageMult += mBounceDamageMultiplierIncrease * Math.pow(mBounceDamageFraction, i);
			}

			damageRaw += bounceDamageRaw;
			damageMultiplier += bounceDamageMult;
		}
		double damage = damageRaw + mAlchemistPotions.getDamage(playerItemStats) * damageMultiplier;
		double potionRadius = mAlchemistPotions.getRadius(playerItemStats);

		double radius = mRadiusMult * potionRadius;
		mCosmetic.explosionEffect(mPlayer, loc, radius);
		if (mEsotericEnhancements != null) {
			mEsotericEnhancements.createPuddle(loc, false, playerItemStats, radius);
		}

		for (LivingEntity mob : new Hitbox.SphereHitbox(loc, radius).getHitMobs()) {
			DamageUtils.damage(
				mPlayer,
				mob,
				new DamageEvent.Metadata(
					DamageEvent.DamageType.MAGIC,
					mInfo.getLinkedSpell(),
					playerItemStats),
				damage,
				true,
				true,
				false);
			GruesomeAlchemy.tryDoEnhancementEffect(mGruesomeAlchemy, mob);
			BrutalAlchemy.tryDoEnhancementEffect(mBrutalAlchemy, mob);

			mAlchemistPotions.applyEffects(mob, false, playerItemStats, true);
			// Two applications at level two
			if (isLevelTwo()) {
				mAlchemistPotions.applyEffects(mob, false, playerItemStats, true);
			}

			if (!EntityUtils.isBoss(mob)) {
				MovementUtils.knockAwayRealistic(loc, mob, 1f, 0.5f, true);
			}
		}
	}

	private static Description<AlchemicalArtillery> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Spend %d potions to launch a bomb that")
				.statValues(stat(a -> a.mCost, POTION_COST))
			.addLine("explodes on contact, dealing damage and")
			.addLine("applying *1* stack of *Brutal* to mobs hit.").styles(WHITE, Alchemist.BRUTAL_COLOR)
			.addLine()
			.addIf((a, p) -> a != null && a.mBaseVelocityMult != VELOCITY_MULTIPLIER, desc -> desc
				.addStat("Velocity: %p")
					.statValues(stat(a -> a.mBaseVelocityMult, VELOCITY_MULTIPLIER)))
			.addStat("Damage: %d1 + %p1 (s) (of potion damage)")
				.statValues(stat(a -> a.mDamageRaw, DAMAGE_RAW_1), stat(a -> a.mDamageMult, DAMAGE_MULTIPLIER_1))
			.addStat("Radius: %p (of potion radius)")
				.statValues(stat(a -> a.mRadiusMult, RADIUS_MULTIPLIER_1))
			.addStat("Cooldown: %t")
				.statValues(StatValue.cooldown(COOLDOWN))
			.addDashedLine();
	}

	private static Description<AlchemicalArtillery> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Alchemical Artillery*'s damage").styles(UNDERLINED)
			.addLine("and radius, and it now applies *2* stacks").styles(WHITE)
			.addLine("of *Brutal*.").styles(Alchemist.BRUTAL_COLOR)
			.addLine()
			.addStatComparison("Damage: %d1 + %p1 -> %d2 + %p2 (s)")
				.statValues(stat(DAMAGE_RAW_1), stat(DAMAGE_MULTIPLIER_1), stat(a -> a.mDamageRaw, DAMAGE_RAW_2), stat(a -> a.mDamageMult, DAMAGE_MULTIPLIER_2))
			.addStatComparison("Radius: %p1 -> %p2")
				.statValues(stat(RADIUS_MULTIPLIER_1), stat(a -> a.mRadiusMult,	 RADIUS_MULTIPLIER_2))
			.addDashedLine();
	}

	private static Description<AlchemicalArtillery> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("The bomb bounces off of terrain up to %d times.")
				.statValues(stat(a -> a.mMaxBounceCount, ENHANCEMENT_MAX_BOUNCES))
			.addLine()
			.addLine("Each time it bounces, its damage increases.")
			.addLine("Subsequent bounces give %p as much bonus")
				.statValues(stat(a -> a.mBounceDamageFraction, ENHANCEMENT_BOUNCE_DAMAGE_FRACTION))
			.addLine("damage as the previous bounce.")
			.addLine()
			.addStat("Bonus Damage: +%d + %p (s) on first bounce")
				.statValues(stat(a -> a.mBounceDamageRawIncrease, ENHANCEMENT_ADDED_DAMAGE_RAW_PER_BOUNCE), stat(a -> a.mBounceDamageMultiplierIncrease, ENHANCEMENT_ADDED_DAMAGE_MULTIPLIER_PER_BOUNCE))
			.addDashedLine();
	}
}
