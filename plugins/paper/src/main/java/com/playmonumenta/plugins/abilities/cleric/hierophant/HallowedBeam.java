package com.playmonumenta.plugins.abilities.cleric.hierophant;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.hierophant.HallowedBeamCS;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Grappling;
import com.playmonumenta.plugins.itemstats.enchantments.PointBlank;
import com.playmonumenta.plugins.itemstats.enchantments.Recoil;
import com.playmonumenta.plugins.itemstats.enchantments.Sniper;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class HallowedBeam extends MultipleChargeAbility {
	// Feature flags
	private static final boolean ENABLE_OPTIMAL_MOB_TARGETING = false;
	private static final boolean CONE_AIM_ASSIST = true;

	private static final int HALLOWED_1_MAX_CHARGES = 2;
	private static final int HALLOWED_2_MAX_CHARGES = 3;
	private static final int HALLOWED_1_COOLDOWN = 20 * 16;
	private static final int HALLOWED_2_COOLDOWN = 20 * 12;
	private static final int CAST_CLICK_DELAY = 5;
	private static final double HALLOWED_HEAL_PERCENT = 0.3;
	private static final double HALLOWED_DAMAGE_REDUCTION_PERCENT = -0.1;
	private static final int HALLOWED_DAMAGE_REDUCTION_DURATION = 20 * 5;
	private static final String PERCENT_DAMAGE_RESIST_EFFECT_NAME = "HallowedPercentDamageResistEffect";
	private static final int HALLOWED_RADIUS = 4;
	private static final int HALLOWED_UNDEAD_STUN = 20; // 20 * 1
	private static final int HALLOWED_LIVING_STUN = 20 * 2;
	private static final int CAST_RANGE = 30;
	private static final double HEALING_THRESHOLD = 0.995;
	// How much the hitbox should expand for these entities
	// Code assumes hostile ray trace size is below player ray trace size
	private static final double RAY_SIZE_STEP = 0.15;
	private static final double MAX_RAY_SIZE = 0.75;
	private static final double MAY_RAY_SIZE_HOSTILE = 0.45;
	private static final double CONE_ANGLE_SIZE = Math.toRadians(12.5);
	private static final double CONE_ANGLE_SIZE_HOSTILE = Math.toRadians(5);
	private static final double CONE_ANGLE_HOSTILE_COS = CONE_ANGLE_SIZE_HOSTILE >= Math.PI ? -1 : FastUtils.cos(CONE_ANGLE_SIZE_HOSTILE);
	private static final String MODE_SCOREBOARD = "HallowedBeamMode";

	public static final String CHARM_DAMAGE = "Hallowed Beam Damage";
	public static final String CHARM_COOLDOWN = "Hallowed Beam Cooldown";
	public static final String CHARM_HEAL = "Hallowed Beam Healing";
	public static final String CHARM_DISTANCE = "Hallowed Beam Distance";
	public static final String CHARM_STUN = "Hallowed Beam Stun Duration";
	public static final String CHARM_RESISTANCE = "Hallowed Beam Resistance";
	public static final String CHARM_RESISTANCE_DURATION = "Hallowed Beam Resistance Duration";
	public static final String CHARM_CHARGE = "Hallowed Beam Charge";
	public static final String CHARM_HEALING_PERCENT_THRESHOLD = "Hallowed Beam Healing Threshold";

	public static final AbilityInfo<HallowedBeam> INFO =
		new AbilityInfo<>(HallowedBeam.class, "Hallowed Beam", HallowedBeam::new)
			.linkedSpell(ClassAbility.HALLOWED_BEAM)
			.scoreboardId("HallowedBeam")
			.shorthandName("HB")
			.descriptions(
				("Left-click with a projectile weapon while looking directly at a player or mob within %s blocks to shoot a beam of light. " +
					"If aimed at a player, the beam instantly heals them for %s%% of their max health, knocking back enemies within %s blocks. " +
					"If aimed at an Undead, it instantly deals magic damage equal to your projectile damage to the target, and stuns them for %ss. " +
					"If aimed at a non-undead mob, it instantly stuns them for %ss. %s charges. " +
					"Swap hands while holding a projectile weapon will change the mode of Hallowed Beam between 'Default' (default), " +
					"'Healing' (only heals players, does not work on mobs), and 'Attack' (only applies mob effects, does not heal). " +
					"This skill can only apply Recoil twice before touching the ground. Cooldown: %ss each charge.")
					.formatted((long) CAST_RANGE,
						StringUtils.multiplierToPercentage(HALLOWED_HEAL_PERCENT),
						(long) HALLOWED_RADIUS,
						StringUtils.ticksToSeconds(HALLOWED_UNDEAD_STUN),
						StringUtils.ticksToSeconds(HALLOWED_LIVING_STUN),
						(long) HALLOWED_1_MAX_CHARGES,
						StringUtils.ticksToSeconds(HALLOWED_1_COOLDOWN)),
				("Hallowed Beam has %s charges (and can apply Recoil three times before touching the ground), " +
					"the cooldown is reduced to %ss, and players healed by it gain +%s damage resistance for %ss.")
					.formatted((long) HALLOWED_2_MAX_CHARGES,
						StringUtils.ticksToSeconds(HALLOWED_2_COOLDOWN),
						StringUtils.multiplierToPercentageWithSign(Math.abs(HALLOWED_DAMAGE_REDUCTION_PERCENT)),
						StringUtils.ticksToSeconds(HALLOWED_DAMAGE_REDUCTION_DURATION)))
			.simpleDescription("Heal a targeted player, damage a targeted Undead, or stun a targeted non-Undead from a distance.")
			.cooldown(HALLOWED_1_COOLDOWN, HALLOWED_2_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", HallowedBeam::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK),
				AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.addTrigger(new AbilityTriggerInfo<>("swapMode", "swap mode", HallowedBeam::swapMode, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false),
				AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.displayItem(Material.BOW);

	private @Nullable Crusade mCrusade;

	private enum Mode {
		DEFAULT(0, "Default"),
		HEALING(1, "Healing"),
		ATTACK(2, "Attack");

		public final int mScore;
		private final String mLabel;

		Mode(int score, String label) {
			mScore = score;
			mLabel = label;
		}
	}

	private enum Target {
		PLAYER,
		HOSTILE,
		NONE
	}

	private Mode mMode = Mode.DEFAULT;
	private int mLastCastTicks = 0;
	private int mLastTryTicks = 0;
	private final HallowedBeamCS mCosmetic;
	private final double mHealingThreshold;
	private final double mRange;
	// Targeting
	private final Predicate<Entity> mPlayerFilter;
	private final Predicate<Entity> mHostileFilter;

	public HallowedBeam(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxCharges = (int) CharmManager.getLevel(player, CHARM_CHARGE) + (isLevelOne() ? HALLOWED_1_MAX_CHARGES : HALLOWED_2_MAX_CHARGES);
		mCharges = getTrackedCharges();
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new HallowedBeamCS());
		double healingThreshold = CharmManager.getLevelPercentDecimal(player, CHARM_HEALING_PERCENT_THRESHOLD);
		if (healingThreshold <= 0) {
			healingThreshold = HEALING_THRESHOLD;
		}
		// Ensure that the charm cannot prevent healing below 70% health or above 100% health
		mHealingThreshold = Math.max(0.7, Math.min(1.0, healingThreshold));
		mRange = CharmManager.getRadius(mPlayer, CHARM_DISTANCE, CAST_RANGE);
		if (player != null) {
			int modeIndex = ScoreboardUtils.getScoreboardValue(player, MODE_SCOREBOARD).orElse(0);
			mMode = Mode.values()[Math.max(0, Math.min(modeIndex, Mode.values().length - 1))];
		}

		mPlayerFilter = e -> e instanceof Player p && p != mPlayer && p.getGameMode() != GameMode.SPECTATOR && !p.isDead() && p.isValid()
		// Do not heal if health is full
		&& getPercentHealth(p) <= mHealingThreshold
		// Do not heal if there is a custom effect preventing heal
		&& Plugin.getInstance().mEffectManager.getEffects(p, PercentHeal.class).stream()
			.filter(percentHeal -> percentHeal.getValue() < -0.995).findAny().isEmpty();
		mHostileFilter = e -> e instanceof LivingEntity le && EntityUtils.isHostileMob(le) && !ScoreboardUtils.checkTag(le, AbilityUtils.IGNORE_TAG) && !le.isDead() && le.isValid();

		Bukkit.getScheduler().runTask(plugin,
			() -> mCrusade = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, Crusade.class));
	}

	public double getPercentHealth(LivingEntity le) {
		return le.getHealth() / EntityUtils.getMaxHealth(le);
	}

	public boolean cast() {
		// optimization to not do checks if the double click protection is active
		// do not attempt casting twice in the same tick
		final int ticks = Bukkit.getServer().getCurrentTick();
		if (mLastTryTicks == ticks || ticks - mLastCastTicks <= CAST_CLICK_DELAY || getCharges() <= 0) {
			return false;
		}
		mLastTryTicks = ticks;

		final World world = mPlayer.getWorld();
		final Location playerEyeLocation = mPlayer.getEyeLocation();
		final Vector playerEyeVector = playerEyeLocation.toVector();
		final Vector playerLookDirection = NmsUtils.getVersionAdapter().getActualDirection(mPlayer);
		playerEyeLocation.setDirection(playerLookDirection);

		Predicate<Entity> mainFilter = e -> false; // Filter for the main rayTrace
		Predicate<Entity> coneFilter = e -> false; // Filter for the cone rayTrace
		switch (mMode) {
			case HEALING: {
				mainFilter = mPlayerFilter;
				coneFilter = mPlayerFilter;
				break;
			}
			case ATTACK: {
				mainFilter = mHostileFilter;
				if (ENABLE_OPTIMAL_MOB_TARGETING) {
					coneFilter = mHostileFilter;
				}
				break;
			}
			case DEFAULT:
			default: {
				mainFilter = mHostileFilter.or(mPlayerFilter);
				coneFilter = ENABLE_OPTIMAL_MOB_TARGETING ? mHostileFilter.or(mPlayerFilter) : mPlayerFilter;
				break;
			}
		}

		double rayTraceSize = 0.0;
		double rayTraceStep = RAY_SIZE_STEP;
		RayTraceResult result;
		// Target entity
		@Nullable LivingEntity targetEntity = null;
		// The set of entities that have already been checked in the raytrace
		Set<UUID> ignoredEntites = new HashSet<>();
		final Predicate<Entity> ignoredFilter = e -> !ignoredEntites.contains(e.getUniqueId());
		// Map of entity uuid to their raytrace hit location
		Map<UUID, Vector> hitLocationEntityMap = new LinkedHashMap<>();
		// Set of entities that pass validations
		Set<LivingEntity> hitEntities = new LinkedHashSet<>();
		@Nullable Vector lastRayTraceHitVector = null;

		// Simple raytrace (with no hitbox expansion) for more precision when close to the enemy
		while (true) {
			result = world.rayTrace(playerEyeLocation, playerLookDirection, mRange, FluidCollisionMode.NEVER, true, rayTraceSize, ignoredFilter.and(mainFilter));
			if (result == null || result.getHitEntity() == null) {
				if (result != null) {
					lastRayTraceHitVector = result.getHitPosition();
				}
				if (rayTraceSize >= MAX_RAY_SIZE) {
					break;
				}
				if (mMode == Mode.ATTACK && rayTraceSize >= MAY_RAY_SIZE_HOSTILE) {
					break;
				}
				rayTraceSize += rayTraceStep;
				continue;
			}
			final Entity hitEntity = result.getHitEntity();
			final UUID uuid = hitEntity.getUniqueId();
			// store last hit location for use in cone check
			lastRayTraceHitVector = result.getHitPosition();
			ignoredEntites.add(uuid);
			if (hitEntity instanceof final LivingEntity livingEntity) {
				Target type = getTargetType(livingEntity);
				if (type == Target.NONE) {
					continue;
				}
				if (type == Target.HOSTILE && rayTraceSize > MAY_RAY_SIZE_HOSTILE) {
					continue;
				}

				// TODO: Enable this check if players can somehow use hallowed beam through walls
				if (rayTraceSize >= 1) {
					// this check ignores entities but is more precise because it uses the hitLocation instead of the player's eyeLocation
					final Vector boundingBoxVector = getClosestPointOnBoundingBox(livingEntity.getBoundingBox(), lastRayTraceHitVector);
					final Vector hitLocationVector = VectorUtils.getDirectionTo(boundingBoxVector, lastRayTraceHitVector);
					final RayTraceResult raytraceBlockResult = world.rayTraceBlocks(lastRayTraceHitVector.toLocation(world), hitLocationVector, rayTraceSize, FluidCollisionMode.NEVER, true);
					if (raytraceBlockResult != null && raytraceBlockResult.getHitBlock() != null) {
						continue;
					}
				}

				hitLocationEntityMap.put(uuid, lastRayTraceHitVector);
				hitEntities.add(livingEntity);
			}
		}

		boolean usingCone = false;
		if (CONE_AIM_ASSIST && hitEntities.isEmpty() && !(mMode == Mode.ATTACK && !ENABLE_OPTIMAL_MOB_TARGETING)) {
			usingCone = true;
			double mAngle = mMode == Mode.ATTACK ? CONE_ANGLE_SIZE_HOSTILE : CONE_ANGLE_SIZE;
			final Hitbox.ApproximateFreeformHitbox coneHitbox = Hitbox.approximateCone(playerEyeLocation, mRange, mAngle);
			final Set<LivingEntity> coneEntities = coneHitbox.getHitEntities(coneFilter).stream().map(e -> (LivingEntity) e).collect(Collectors.toSet());
			for (final LivingEntity livingEntity : coneEntities) {
				if (hitEntities.contains(livingEntity)) {
					continue;
				}
				Target type = getTargetType(livingEntity);
				if (type == Target.NONE) {
					continue;
				}

				// Hostiles do not get aim assist unless feature flag is on
				final BoundingBox boundingBox = livingEntity.getBoundingBox();
				if (mMode != Mode.ATTACK && type == Target.HOSTILE) {
					if (!ENABLE_OPTIMAL_MOB_TARGETING) {
						continue;
					}
					if (CONE_ANGLE_SIZE_HOSTILE != CONE_ANGLE_SIZE) {
						final Vector boundingBoxVector = getClosestPointOnBoundingBox(boundingBox, playerEyeVector);
						final double distanceFromCenter = boundingBoxVector.subtract(playerEyeVector).normalize().dot(playerLookDirection);
						if (distanceFromCenter > CONE_ANGLE_HOSTILE_COS) {
							continue;
						}
					}
				}

				// This check is less precise and can have false positives, but should work in 90% of cases
				final Vector boundingBoxVector = getClosestPointOnBoundingBox(boundingBox, playerEyeVector);
				final Vector hitLocationVector = VectorUtils.getDirectionTo(boundingBoxVector, playerEyeVector);
				// This is technically an nerf, but it allows you to adjust the aim-assist's pitch
				// TODO: fix this - kinda is a hacky workaround
				// hitLocationVector.setY(playerLookDirection.getY());
				final Predicate<Entity> filter = e -> e == livingEntity;
				result = world.rayTrace(playerEyeLocation, hitLocationVector, mRange, FluidCollisionMode.NEVER, true, 0.0, filter);
				if (result == null || result.getHitEntity() == null || result.getHitBlock() != null) {
					continue;
				}

				final Vector entityHitLocation = result.getHitPosition();
				final UUID uuid = livingEntity.getUniqueId();
				hitLocationEntityMap.put(uuid, entityHitLocation);
				hitEntities.add(livingEntity);
			}
		}

		if (hitEntities.isEmpty()) {
			return false;
		}

		Comparator<LivingEntity> distanceComparator = new DistanceFromCenterComparator(playerEyeLocation);
		if (mMode != Mode.DEFAULT) {
			Comparator<LivingEntity> comparator;
			if (mMode == Mode.HEALING) {
				comparator = new PlayerComparator();
			} else {
				comparator = new HostileComparator();
			}
			if (usingCone) {
				comparator = comparator.thenComparing(distanceComparator);
			}
			targetEntity = hitEntities.stream().filter(mainFilter).max(comparator).orElse(null);
		}

		if (targetEntity == null && mMode == Mode.DEFAULT) {
			if (usingCone) {
				targetEntity = hitEntities.stream().max(distanceComparator).orElse(null);
			} else {
				targetEntity = hitEntities.stream().findFirst().orElse(null);
			}
			// Use PlayerPriorityComparator if you want to prioritize players over mobs
			// targetEntity = hitEntities.stream().max(new PlayerPriorityComparator().thenComparing(distanceComparator)).orElse(null);
		}

		// cooldown check
		if (targetEntity == null || !consumeCharge()) {
			return false;
		}
		mLastCastTicks = ticks;

		// get closest y-level on target entity (extremely necessary)
		final Vector hitLocation = hitLocationEntityMap.get(targetEntity.getUniqueId());
		final Vector targetHitLocation = hitLocation != null ? hitLocation
		: lastRayTraceHitVector != null ? lastRayTraceHitVector
		: playerEyeLocation.toVector();
		final Location targetLocation = getClosestPointOnBoundingBox(targetEntity.getBoundingBox(), targetHitLocation).toLocation(world);

		if (targetEntity instanceof final Player player) {
			healPlayer(player, true);
			// knockaway enemies from the hit location
			final Location knockAwayLocation = player.getLocation();
			for (LivingEntity le : EntityUtils.getNearbyMobs(knockAwayLocation, HALLOWED_RADIUS)) {
				MovementUtils.knockAway(knockAwayLocation, le, 0.65f, true);
			}
			mCosmetic.beamHealTarget(mPlayer, player, targetLocation);
			mCosmetic.beamHealEffect(mPlayer, player, playerLookDirection, mRange, targetLocation);
		} else {
			damageEntity(targetEntity, true, targetLocation);
			mCosmetic.beamHarm(mPlayer, targetEntity, playerLookDirection, mRange, targetLocation);
		}
		applyRecoil();
		applyGrappling(targetEntity);

		return true;
	}

	private final class DistanceFromCenterComparator implements Comparator<LivingEntity> {
		final Vector mVector;
		final Vector mDirection;

		private DistanceFromCenterComparator(Location playerEyeLocation) {
			mVector = playerEyeLocation.toVector();
			mDirection = playerEyeLocation.getDirection();
		}

		@Override
		public int compare(LivingEntity a, LivingEntity b) {
			final Vector vectorA = getClosestPointOnBoundingBox(a.getBoundingBox(), mVector);
			final Vector vectorB = getClosestPointOnBoundingBox(b.getBoundingBox(), mVector);
			final double numA = vectorA.clone().subtract(mVector).normalize().dot(mDirection);
			final double numB = vectorB.clone().subtract(mVector).normalize().dot(mDirection);
			double diffA = 1 - Math.abs(numA);
			double diffB = 1 - Math.abs(numB);
			double difference = Math.abs(diffA - diffB);
			if (difference >= 0.01) {
				if (diffA < diffB) {
					return 1;
				} else if (diffA > diffB) {
					return -1;
				}
			}
			final double distanceA = vectorA.distanceSquared(mVector);
			final double distanceB = vectorB.distanceSquared(mVector);
			return -Double.compare(distanceA, distanceB);
		}
	}

	/*
	private static final class PlayerPriorityComparator implements Comparator<LivingEntity> {
		@Override
		public int compare(LivingEntity a, LivingEntity b) {
			if (a instanceof Player && b instanceof Player) {
				return 0;
			} else if (a instanceof Player) {
				return 1;
			} else if (b instanceof Player) {
				return -1;
			}
			return 0;
		}
	}
	*/

	private final class PlayerComparator implements Comparator<LivingEntity> {
		@Override
		public int compare(LivingEntity a, LivingEntity b) {
			return -Double.compare(getPercentHealth(a), getPercentHealth(b));
		}
	}

	private final class HostileComparator implements Comparator<LivingEntity> {
		@Override
		public int compare(LivingEntity a, LivingEntity b) {
			if (!ENABLE_OPTIMAL_MOB_TARGETING) {
				return 0;
			}
			return Boolean.compare(Crusade.enemyTriggersAbilities(a, mCrusade), Crusade.enemyTriggersAbilities(b, mCrusade));
		}
	}

	private Target getTargetType(LivingEntity livingEntity) {
		// Simple sanity validation predicate
		final boolean isPlayer = mPlayerFilter.test(livingEntity);
		final boolean isHostile = mHostileFilter.test(livingEntity);
		if (mMode == Mode.HEALING && isPlayer) {
			return Target.PLAYER;
		} else if (mMode == Mode.ATTACK && isHostile) {
			return Target.HOSTILE;
		} else if (mMode == Mode.DEFAULT && (isPlayer || isHostile)) {
			return isPlayer ? Target.PLAYER : Target.HOSTILE;
		}
		return Target.NONE;
	}

	private Vector getClosestPointOnBoundingBox(BoundingBox box, Vector eye) {
		return getClosestPointOnBoundingBox(box, eye, false);
	}

	private Vector getClosestPointOnBoundingBox(BoundingBox box, Vector eye, boolean shouldClampXZ) {
		Vector vector = box.getCenter();
		if (shouldClampXZ) {
			vector.setX(Math.max(box.getMinX(), Math.min(box.getMaxX(), eye.getX())));
			vector.setZ(Math.max(box.getMinZ(), Math.min(box.getMaxZ(), eye.getZ())));
		}
		vector.setY(Math.max(box.getMinY(), Math.min(box.getMaxY(), eye.getY())));
		return vector;
	}

	private double calculateDamage(LivingEntity target) {
		final PlayerInventory inventory = mPlayer.getInventory();
		final ItemStack inMainHand = inventory.getItemInMainHand();

		double damage = ItemStatUtils.getAttributeAmount(inMainHand, AttributeType.PROJECTILE_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND);
		damage += Sniper.apply(mPlayer, target, ItemStatUtils.getEnchantmentLevel(inMainHand, EnchantmentType.SNIPER));
		damage += PointBlank.apply(mPlayer, target, ItemStatUtils.getEnchantmentLevel(inMainHand, EnchantmentType.POINT_BLANK));
		// Hallowed Beam has special case for proj damage scaling in ProjectileDamageMultiply. See AbilityUtils for more info
		damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage);
		// Cap to not damage negative amounts
		damage = Math.max(0, damage);
		return damage;
	}

	private void healPlayer(Player player, boolean mainTarget) {
		double healAmount = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEAL, EntityUtils.getMaxHealth(player) * HALLOWED_HEAL_PERCENT);
		if (healAmount > 0) {
			PlayerUtils.healPlayer(mPlugin, player, healAmount, mPlayer);
		}

		if (isLevelTwo() && mainTarget) {
			double resistance = HALLOWED_DAMAGE_REDUCTION_PERCENT - CharmManager.getLevelPercentDecimal(mPlayer, CHARM_RESISTANCE);
			int duration = CharmManager.getDuration(mPlayer, CHARM_RESISTANCE_DURATION, HALLOWED_DAMAGE_REDUCTION_DURATION);
			mPlugin.mEffectManager.addEffect(player, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(duration, resistance));
		}
	}

	private void damageEntity(LivingEntity livingEntity, final boolean mainTarget, @Nullable Location targetLocation) {
		if (targetLocation == null) {
			targetLocation = LocationUtils.getEntityCenter(livingEntity);
		}
		int stunDuration;
		if (Crusade.enemyTriggersAbilities(livingEntity, mCrusade)) {
			double damage = calculateDamage(livingEntity);
			if (!mainTarget) {
				damage *= 0.5;
			}
			if (damage > 0) {
				DamageUtils.damage(mPlayer, livingEntity, DamageType.MAGIC, damage, mInfo.getLinkedSpell(), true, true);
			}

			if (mainTarget && mPlugin.mItemStatManager.getEnchantmentLevel(mPlayer, EnchantmentType.FIRE_ASPECT) > 0) {
				EntityUtils.applyFire(mPlugin, 20 * 15, livingEntity, mPlayer);
			}

			stunDuration = HALLOWED_UNDEAD_STUN;

			mCosmetic.beamHarmCrusade(mPlayer, livingEntity, targetLocation);
		} else {
			stunDuration = HALLOWED_LIVING_STUN;

			mCosmetic.beamHarmOther(mPlayer, livingEntity, targetLocation);
		}

		if (mainTarget) {
			EntityUtils.applyStun(mPlugin, CharmManager.getDuration(mPlayer, CHARM_STUN, stunDuration), livingEntity);
		}

		Crusade.addCrusadeTag(livingEntity, mCrusade);
	}

	private void applyRecoil() {
		ItemStack item = mPlayer.getInventory().getItemInMainHand();
		double recoil = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.RECOIL);
		if (recoil > 0
			&& !EntityUtils.isRecoilDisable(mPlugin, mPlayer, mMaxCharges)
			&& !mPlayer.isSneaking()
			&& !ZoneUtils.hasZoneProperty(mPlayer, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
			Recoil.applyRecoil(mPlayer, recoil);
			EntityUtils.applyRecoilDisable(mPlugin, 9999, (int) EntityUtils.getRecoilDisableAmount(mPlugin, mPlayer) + 1, mPlayer);
		}
	}

	private void applyGrappling(LivingEntity target) {
		ItemStack item = mPlayer.getInventory().getItemInMainHand();
		double grappling = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.GRAPPLING);
		if (grappling > 0
			&& !EntityUtils.isRecoilDisable(mPlugin, mPlayer, mMaxCharges)
			&& !ZoneUtils.hasZoneProperty(mPlayer, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
			if (getPlayer().isSneaking()) {
				Grappling.pullMob(mPlayer, target, Grappling.MOB_HORIZONTAL_SPEED, grappling);
			} else {
				Grappling.pullMob(target, mPlayer, Grappling.PLAYER_HORIZONTAL_SPEED, grappling);
			}
			EntityUtils.applyRecoilDisable(mPlugin, 9999, (int) EntityUtils.getRecoilDisableAmount(mPlugin, mPlayer) + 1, mPlayer);
		}
	}

	public boolean swapMode() {
		if (mMode == Mode.DEFAULT) {
			mMode = Mode.HEALING;
		} else if (mMode == Mode.HEALING) {
			mMode = Mode.ATTACK;
		} else {
			mMode = Mode.DEFAULT;
		}
		sendActionBarMessage(ClassAbility.HALLOWED_BEAM.getName() + " Mode: " + mMode.mLabel);
		ScoreboardUtils.setScoreboardValue(mPlayer, MODE_SCOREBOARD, mMode.mScore);
		ClientModHandler.updateAbility(mPlayer, this);
		return true;
	}

	@Override
	public @Nullable String getMode() {
		return mMode.name().toLowerCase(Locale.ROOT);
	}
}
