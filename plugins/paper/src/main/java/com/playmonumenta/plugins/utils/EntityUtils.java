package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.CrowdControlImmunityBoss;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.effects.Bleed;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.Paralyze;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.effects.RecoilDisable;
import com.playmonumenta.plugins.effects.SplitArrowIframesEffect;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.enchantments.FireProtection;
import com.playmonumenta.plugins.itemstats.enchantments.Inferno;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;


public class EntityUtils {

	private static final EnumSet<EntityType> UNDEAD_MOBS = EnumSet.of(
		EntityType.ZOMBIE,
		EntityType.ZOMBIE_VILLAGER,
		EntityType.ZOMBIFIED_PIGLIN,
		EntityType.HUSK,
		EntityType.SKELETON,
		EntityType.WITHER_SKELETON,
		EntityType.STRAY,
		EntityType.WITHER,
		EntityType.ZOMBIE_HORSE,
		EntityType.SKELETON_HORSE,
		EntityType.PHANTOM,
		EntityType.DROWNED
	);

	private static final EnumSet<EntityType> BEAST_MOBS = EnumSet.of(
		EntityType.CREEPER,
		EntityType.BLAZE,
		EntityType.GHAST,
		EntityType.ENDERMAN,
		EntityType.ENDER_DRAGON,
		EntityType.WOLF,
		EntityType.OCELOT,
		EntityType.HOGLIN,
		EntityType.RAVAGER,
		EntityType.SLIME,
		EntityType.MAGMA_CUBE,
		EntityType.SHULKER,
		EntityType.SPIDER,
		EntityType.CAVE_SPIDER,
		EntityType.SILVERFISH,
		EntityType.ENDERMITE,
		EntityType.AXOLOTL,
		EntityType.BEE,
		EntityType.POLAR_BEAR,
		EntityType.BAT,
		EntityType.CAT,
		EntityType.CHICKEN,
		EntityType.COW,
		EntityType.DONKEY,
		EntityType.FOX,
		EntityType.GOAT,
		EntityType.GLOW_SQUID,
		EntityType.HORSE,
		EntityType.LLAMA,
		EntityType.MULE,
		EntityType.MUSHROOM_COW,
		EntityType.PANDA,
		EntityType.PARROT,
		EntityType.PIG,
		EntityType.RABBIT,
		EntityType.SHEEP,
		EntityType.DOLPHIN,
		EntityType.GUARDIAN,
		EntityType.ELDER_GUARDIAN,
		EntityType.SQUID,
		EntityType.TURTLE,
		EntityType.COD,
		EntityType.SALMON,
		EntityType.TROPICAL_FISH,
		EntityType.PUFFERFISH
	);

	// This list is hardcoded for Crusade description & Duelist advancement
	private static final EnumSet<EntityType> HUMANLIKE_MOBS = EnumSet.of(
		EntityType.EVOKER,
		EntityType.ILLUSIONER,
		EntityType.PILLAGER,
		EntityType.VINDICATOR,

		EntityType.VEX,
		EntityType.WITCH,

		EntityType.PIGLIN,
		EntityType.PIGLIN_BRUTE,

		EntityType.IRON_GOLEM,
		EntityType.SNOWMAN,

		EntityType.GIANT
	);

	private static final EnumSet<EntityType> FLYING_MOBS = EnumSet.of(
		EntityType.BEE,
		EntityType.BLAZE,
		EntityType.BAT,
		EntityType.WITHER,
		EntityType.GHAST,
		EntityType.PARROT,
		EntityType.PHANTOM,
		EntityType.VEX
	);

	private static final EnumSet<EntityType> WATER_MOBS = EnumSet.of(
		EntityType.AXOLOTL,
		EntityType.DOLPHIN,
		EntityType.DROWNED,
		EntityType.GLOW_SQUID,
		EntityType.GUARDIAN,
		EntityType.ELDER_GUARDIAN,
		EntityType.SQUID,
		EntityType.TURTLE,
		EntityType.COD,
		EntityType.SALMON,
		EntityType.TROPICAL_FISH,
		EntityType.PUFFERFISH
	);

	private static final String COOLING_ATTR_NAME = "CoolingSlownessAttr";
	private static final String STUN_ATTR_NAME = "StunSlownessAttr";
	private static final String IGNORE_TAUNT_TAG = "taunt_ignore";
	private static final Map<LivingEntity, Integer> COOLING_MOBS = new HashMap<>();
	private static final Map<LivingEntity, Integer> STUNNED_MOBS = new HashMap<>();
	private static final Map<LivingEntity, Integer> SILENCED_MOBS = new HashMap<>();
	private static BukkitRunnable mobsTracker = null;

	private static final Particle.DustOptions STUN_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 100), 1.0f);
	private static final Particle.DustOptions SILENCE_COLOR = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);
	private static final Particle.DustOptions TAUNT_COLOR = new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.0f);

	private static void startTracker(Plugin plugin) {
		mobsTracker = new BukkitRunnable() {
			int mRotation = 0;

			@Override
			public void run() {
				mRotation += 20;

				Iterator<Map.Entry<LivingEntity, Integer>> coolingIter = COOLING_MOBS.entrySet().iterator();
				Iterator<Map.Entry<LivingEntity, Integer>> stunnedIter = STUNNED_MOBS.entrySet().iterator();
				Iterator<Map.Entry<LivingEntity, Integer>> silencedIter = SILENCED_MOBS.entrySet().iterator();

				while (coolingIter.hasNext()) {
					Map.Entry<LivingEntity, Integer> cooling = coolingIter.next();
					LivingEntity mob = cooling.getKey();
					cooling.setValue(cooling.getValue() - 1);

					if (cooling.getValue() <= 0 || mob.isDead() || !mob.isValid()) {
						AttributeInstance ai = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
						if (ai != null) {
							for (AttributeModifier mod : ai.getModifiers()) {
								if (mod != null && mod.getName().equals(COOLING_ATTR_NAME)) {
									ai.removeModifier(mod);
								}
							}
						}

						if (mob instanceof Mob) {
							((Mob) mob).setTarget(getNearestPlayer(mob.getLocation(), mob.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).getValue()));
						}

						coolingIter.remove();
					}
				}

				while (stunnedIter.hasNext()) {
					Map.Entry<LivingEntity, Integer> stunned = stunnedIter.next();
					LivingEntity mob = stunned.getKey();
					stunned.setValue(stunned.getValue() - 1);

					if (mob instanceof Vex || mob instanceof Flying) {
						mob.setVelocity(new Vector(0, 0, 0));
					}

					double angle = Math.toRadians(mRotation);
					Location l = mob.getLocation();
					l.add(FastUtils.cos(angle) * 0.5, mob.getHeight(), FastUtils.sin(angle) * 0.5);
					new PartialParticle(Particle.REDSTONE, l, 5, 0, 0, 0, STUN_COLOR).spawnAsEnemyBuff();

					if (stunned.getValue() <= 0 || mob.isDead() || !mob.isValid()) {
						AttributeInstance ai = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
						if (ai != null) {
							for (AttributeModifier mod : ai.getModifiers()) {
								if (mod != null && mod.getName().equals(STUN_ATTR_NAME)) {
									ai.removeModifier(mod);
								}
							}
						}
						stunnedIter.remove();
					}
				}

				while (silencedIter.hasNext()) {
					Map.Entry<LivingEntity, Integer> silenced = silencedIter.next();
					LivingEntity mob = silenced.getKey();
					silenced.setValue(silenced.getValue() - 1);

					double angle = Math.toRadians(mRotation);
					Location l = mob.getLocation();
					l.add(FastUtils.cos(angle) * 0.5, mob.getHeight(), FastUtils.sin(angle) * 0.5);
					new PartialParticle(Particle.REDSTONE, l, 5, 0, 0, 0, SILENCE_COLOR).spawnAsEnemyBuff();

					if (silenced.getValue() <= 0 || mob.isDead() || !mob.isValid()) {
						silencedIter.remove();
					}
				}
			}
		};

		mobsTracker.runTaskTimer(plugin, 0, 1);
	}

	// Affected by Smite
	public static boolean isUndead(LivingEntity mob) {
		return UNDEAD_MOBS.contains(mob.getType());
	}

	// Affected by Slayer
	public static boolean isBeast(LivingEntity mob) {
		return BEAST_MOBS.contains(mob.getType());
	}

	// Affected by Duelist
	public static boolean isHumanlike(LivingEntity mob) {
		return HUMANLIKE_MOBS.contains(mob.getType());
	}

	public static boolean isFlyingMob(LivingEntity mob) {
		return isFlyingMob(mob.getType());
	}

	public static boolean isFlyingMob(EntityType type) {
		return FLYING_MOBS.contains(type);
	}

	public static boolean isWaterMob(LivingEntity mob) {
		return isWaterMob(mob.getType());
	}

	public static boolean isWaterMob(EntityType type) {
		return WATER_MOBS.contains(type);
	}

	// Affected by Abyssal
	public static boolean isInWater(LivingEntity mob) {
		return LocationUtils.isLocationInWater(mob.getLocation()) || LocationUtils.isLocationInWater(mob.getLocation().subtract(0, 1, 0));
	}

	public static boolean touchesLava(Entity entity) {
		BoundingBox boundingBox = entity.getBoundingBox();
		for (int x = (int) Math.floor(boundingBox.getMinX()); x < boundingBox.getMaxX(); x++) {
			for (int y = (int) Math.floor(boundingBox.getMinY()); y < boundingBox.getMaxY(); y++) {
				for (int z = (int) Math.floor(boundingBox.getMinZ()); z < boundingBox.getMaxZ(); z++) {
					if (entity.getWorld().getBlockAt(x, y, z).getType() == Material.LAVA) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean isElite(Entity entity) {
		Set<String> tags = entity.getScoreboardTags();
		return tags.contains("Elite");
	}

	public static boolean isBoss(Entity entity) {
		Set<String> tags = entity.getScoreboardTags();
		return tags.contains("Boss");
	}

	public static boolean isHostileMob(@Nullable Entity entity) {
		if (entity == null) {
			return false;
		}
		if (!entity.getScoreboardTags().contains("SkillImmune")) {
			if (entity instanceof Monster || entity instanceof Slime || entity instanceof Ghast || entity instanceof PolarBear
				|| entity instanceof Phantom || entity instanceof Shulker || entity instanceof PufferFish
				|| entity instanceof SkeletonHorse || entity instanceof ZombieHorse || entity instanceof Giant
				|| entity instanceof Hoglin || entity instanceof Piglin || entity instanceof Bee) {
				return true;
			} else if (entity instanceof Wolf) {
				return (((Wolf) entity).isAngry() && ((Wolf) entity).getOwner() != null) || entity.getScoreboardTags().contains("boss_targetplayer");
			} else if (entity instanceof Rabbit) {
				return ((Rabbit) entity).getRabbitType().equals(Rabbit.Type.THE_KILLER_BUNNY);
			} else if (entity instanceof Player) {
				return AbilityManager.getManager().isPvPEnabled((Player) entity);
			} else if (entity instanceof Mob) {
				LivingEntity target = ((Mob) entity).getTarget();
				return (target != null && target instanceof Player) || entity.getScoreboardTags().contains("boss_targetplayer") || entity.getScoreboardTags().contains("boss_hostile") || entity.getScoreboardTags().contains("Hostile");
			}
		}

		return false;
	}

	public static boolean isFireResistant(LivingEntity mob) {
		return mob instanceof Blaze || mob instanceof Ghast || mob instanceof MagmaCube || mob instanceof PigZombie || mob instanceof Wither
			|| mob instanceof WitherSkeleton || mob.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE);
	}

	public static boolean isStillLoaded(Entity entity) {
		Location loc = entity.getLocation();
		if (!loc.isChunkLoaded()) {
			return false;
		}

		for (Entity ne : loc.getWorld().getNearbyEntities(loc, 0.75, 0.75, 0.75)) {
			if (ne.getUniqueId().equals(entity.getUniqueId())) {
				return true;
			}
		}

		return false;
	}

	public static @Nullable LivingEntity getEntityAtCursor(Player player, int range, boolean targetPlayers, boolean targetNonPlayers, boolean checkLos) {
		List<Entity> en = player.getNearbyEntities(range, range, range);
		ArrayList<LivingEntity> entities = new ArrayList<>();
		for (Entity e : en) {
			//  Make sure to only get living entities.
			if (e instanceof LivingEntity) {
				//  Make sure we should be targeting this entity.
				if ((targetPlayers && (e instanceof Player ep) && ep.getGameMode() != GameMode.SPECTATOR) || (targetNonPlayers && !(e instanceof Player))) {
					entities.add((LivingEntity) e);
				}
			}
		}

		//  If there's no living entities nearby then we should just leave as there's no reason to continue.
		if (entities.size() == 0) {
			return null;
		}

		BlockIterator bi;
		try {
			bi = new BlockIterator(player, range);
		} catch (IllegalStateException e) {
			return null;
		}

		int bx;
		int by;
		int bz;

		while (bi.hasNext()) {
			Block b = bi.next();
			bx = b.getX();
			by = b.getY();
			bz = b.getZ();

			//  If we want to check Line of sight we want to make sure that the blocks are transparent.
			if (checkLos && BlockUtils.isLosBlockingBlock(b.getType())) {
				break;
			}

			//  Loop through the entities and see if we hit one.
			for (LivingEntity e : entities) {
				Location loc = e.getLocation();
				double ex = loc.getX();
				double ey = loc.getY();
				double ez = loc.getZ();

				if ((bx - 0.75D <= ex) && (ex <= bx + 1.75D)
					&& (bz - 0.75D <= ez) && (ez <= bz + 1.75D)
					&& (by - 1.0D <= ey) && (ey <= by + 2.5D)) {

					//  We got our target.
					return e;
				}
			}
		}

		return null;
	}

	public static Projectile spawnProjectile(LivingEntity player, double yawOffset, double pitchOffset, Vector offset, float speed, Class<? extends Projectile> projectileClass) {
		Location loc = player.getEyeLocation();
		loc.add(offset);

		// Start with the assumption the player is facing due South (yaw 0.0, pitch 0.0, no offset, speed of 1.0
		Vector dir = new Vector(0.0, 0.0, 1.0);
		// Apply pitch/yaw offset to get arrow pattern
		dir = VectorUtils.rotateXAxis(dir, pitchOffset);
		dir = VectorUtils.rotateYAxis(dir, yawOffset);
		// Apply player pitch/yaw to rotate that pattern to match the player's direction
		dir = VectorUtils.rotateXAxis(dir, loc.getPitch());
		dir = VectorUtils.rotateYAxis(dir, loc.getYaw());

		// Change the location's direction to match the arrow's direction
		loc.setDirection(dir);

		World world = player.getWorld();

		// Spawn the arrow at the specified location, direction, and speed
		Projectile projectile = world.spawn(loc, projectileClass);
		projectile.setVelocity(dir.normalize().multiply(speed));
		projectile.setShooter(player);
		return projectile;
	}


	public static List<Projectile> spawnVolley(LivingEntity player, int numProjectiles, float speed, double spacing, Class<? extends Projectile> projectileClass) {
		List<Projectile> projectiles = new ArrayList<>();

		for (int i = 0; i < numProjectiles; i++) {
			double yaw = spacing * (i - (numProjectiles - 1) / 2f);
			Projectile arrow = spawnProjectile(player, yaw, 0.0, new Vector(0, 0, 0), speed, projectileClass);
			projectiles.add(arrow);
		}

		return projectiles;
	}

	public static AreaEffectCloud spawnAreaEffectCloud(World world, Location loc, Collection<PotionEffect> effects, float radius, int duration) {
		AreaEffectCloud cloud = (AreaEffectCloud) world.spawnEntity(loc, EntityType.AREA_EFFECT_CLOUD);

		for (PotionEffect effect : effects) {
			cloud.addCustomEffect(effect, false);
		}

		cloud.setRadius(radius);
		cloud.setDuration(duration);

		return cloud;
	}

	public static ThrownPotion spawnCustomSplashPotion(Player player, ItemStack potionStack, Location loc) {
		ThrownPotion potion = (ThrownPotion) loc.getWorld().spawnEntity(loc.add(0, 0.5, 0), EntityType.SPLASH_POTION);
		potion.setShooter(player);
		potion.setItem(potionStack);

		return potion;
	}

	public static boolean withinRangeOfMonster(Player player, double range) {
		List<Entity> entities = player.getNearbyEntities(range, range, range);
		for (Entity entity : entities) {
			if (isHostileMob(entity) && !entity.getScoreboardTags().contains("summon_ignore")) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns a List of LivingEntity objects in the bounding box with the specified dimensions.
	 * <p>
	 * The List will only include objects with EntityType contained in types. If types is null, only hostile mobs will be included.
	 *
	 * @param loc   Location representing center of the bounding box
	 * @param rx    distance from center to faces perpendicular to x-axis
	 * @param ry    distance from center to faces perpendicular to y-axis
	 * @param rz    distance from center to faces perpendicular to z-axis
	 * @param types List of EntityTypes to be returned, defaults to hostile mobs if null
	 * @return List of LivingEntity objects of the specified type within the bounding box
	 */
	public static List<LivingEntity> getNearbyMobs(Location loc, double rx, double ry, double rz, EnumSet<EntityType> types) {
		Collection<LivingEntity> entities = loc.getWorld().getNearbyEntitiesByType(LivingEntity.class, loc, rx, ry, rz);

		List<LivingEntity> mobs = new ArrayList<>(entities.size());
		for (LivingEntity entity : entities) {
			if (entity.isDead() || !entity.isValid()) {
				continue;
			}

			if (types == null) { // If no types specified
				if (!isHostileMob(entity)) { // Only add hostiles. Not hostile = skip
					continue;
				}
			} else if (!types.contains(entity.getType())) { // If types specified, only add those. Not match = skip
				continue;
			}

			mobs.add(entity);
		}

		return mobs;
	}

	public static List<LivingEntity> getNearbyMobs(Location loc, double rx, double ry, double rz) {
		return getNearbyMobs(loc, rx, ry, rz, null);
	}

	public static List<LivingEntity> getNearbyMobs(Location loc, double radius, LivingEntity getter) {
		List<LivingEntity> list = getNearbyMobs(loc, radius, radius, radius);
		list.remove(getter);
		return list;
	}

	public static List<LivingEntity> getNearbyMobs(Location loc, double radius, LivingEntity getter, boolean ignoreStack) {
		List<LivingEntity> list = getNearbyMobs(loc, radius, radius, radius);
		if (ignoreStack) {
			List<LivingEntity> mobs = new ArrayList<LivingEntity>();
			if (getter.getVehicle() != null) {
				getStackedMobsBelow(getter, mobs);
			}

			if (getter.getPassenger() != null) {
				getStackedMobsAbove(getter, mobs);
			}
			for (LivingEntity mob : mobs) {
				list.remove(mob);
			}
		}
		return list;
	}

	public static List<LivingEntity> getNearbyMobs(Location loc, double radius) {
		return getNearbyMobs(loc, radius, radius, radius);
	}

	public static List<LivingEntity> getNearbyMobs(Location loc, double radius, EnumSet<EntityType> types) {
		return getNearbyMobs(loc, radius, radius, radius, types);
	}

	public static void getStackedMobsAbove(Entity base, List<LivingEntity> prior) {
		if (isHostileMob(base)) {
			prior.add((LivingEntity) base);
		}

		if (base.getPassenger() != null) {
			getStackedMobsAbove(base.getPassenger(), prior);
		}
	}

	public static void getStackedMobsBelow(Entity base, List<LivingEntity> prior) {
		if (isHostileMob(base)) {
			prior.add((LivingEntity) base);
		}

		if (base.getVehicle() != null) {
			getStackedMobsBelow(base.getVehicle(), prior);
		}
	}

	public static List<LivingEntity> getMobsInLine(Location loc, Vector direction, double range, double halfHitboxLength) {
		Set<LivingEntity> nearbyMobs = new HashSet<>(getNearbyMobs(loc, range));
		List<LivingEntity> mobsInLine = new ArrayList<>();

		Vector shift = direction.normalize().multiply(halfHitboxLength);
		BoundingBox hitbox = BoundingBox.of(loc, halfHitboxLength * 2, halfHitboxLength * 2, halfHitboxLength * 2);

		for (double r = 0; r < range; r += halfHitboxLength) {
			Iterator<LivingEntity> iter = nearbyMobs.iterator();
			while (iter.hasNext()) {
				LivingEntity mob = iter.next();
				if (mob.getBoundingBox().overlaps(hitbox)) {
					mobsInLine.add(mob);
					iter.remove();
				}
			}

			hitbox.shift(shift);
		}

		return mobsInLine;
	}

	public static List<Player> getPlayersInLine(Location loc, Vector direction, double range, double halfHitboxLength, Player self) {
		Set<Player> nearbyPlayers = new HashSet<>(PlayerUtils.playersInRange(loc, range, true));
		List<Player> playersInLine = new ArrayList<>();

		Vector shift = direction.normalize().multiply(halfHitboxLength);
		BoundingBox hitbox = BoundingBox.of(loc, halfHitboxLength * 2, halfHitboxLength * 2, halfHitboxLength * 2);

		for (double r = 0; r < range; r += halfHitboxLength) {
			Iterator<Player> iter = nearbyPlayers.iterator();
			while (iter.hasNext()) {
				Player p = iter.next();
				if (p.getName().equals(self.getName())) {
					iter.remove();
					continue;
				}
				if (p.getBoundingBox().overlaps(hitbox)) {
					playersInLine.add(p);
					iter.remove();
				}
			}

			hitbox.shift(shift);
		}

		return playersInLine;
	}

	public static @Nullable LivingEntity getNearestMob(Location loc, double radius, LivingEntity getter) {
		return getNearestMob(loc, getNearbyMobs(loc, radius, getter));
	}

	public static @Nullable LivingEntity getNearestMob(Location loc, double radius) {
		return getNearestMob(loc, getNearbyMobs(loc, radius));
	}

	public static @Nullable LivingEntity getNearestMob(Location loc, List<LivingEntity> nearbyMobs) {
		return nearbyMobs
			.stream()
			.min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(loc)))
			.orElse(null);
	}

	public static @Nullable Player getNearestPlayer(Location loc, double radius) {
		return PlayerUtils.playersInRange(loc, radius, true)
			.stream()
			.min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(loc)))
			.orElse(null);
	}

	/**
	 * Gets players within radius of the location and sorts them by distance.
	 * <p>
	 * <b>WARNING: Distance is sorted from furthest to closest</b>,
	 * i.e. The player 20 blocks away is closer to the front of the list than the player 10 blocks away.
	 * If you want to find the closest players, use the end of the list, the farthest, use the beginning.
	 */
	public static List<Player> getNearestPlayers(Location loc, double radius) {
		List<Player> nearbyPlayers = PlayerUtils.playersInRange(loc, radius, true);
		nearbyPlayers.sort(Comparator.<Player>comparingDouble(e -> e.getLocation().distanceSquared(loc)).reversed());
		return nearbyPlayers;
	}

	public static double vulnerabilityMult(LivingEntity target) {
		if (target instanceof Player) {
			PotionEffect unluck = target.getPotionEffect(PotionEffectType.UNLUCK);
			if (unluck != null) {
				double vulnLevel = 1 + unluck.getAmplifier();

				if (EntityUtils.isBoss(target)) {
					vulnLevel = vulnLevel / 2;
				}

				return 1 + 0.05 * vulnLevel;
			}
		}

		return 1;
	}

	public static @Nullable LivingEntity getNearestHostile(Player player, double range) {
		Location loc = player.getLocation();
		return loc.getNearbyEntitiesByType(LivingEntity.class, range, range, range)
			.stream()
			.filter(e -> e.isValid() && isHostileMob(e))
			.min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(loc)))
			.orElse(null);
	}

	private static final String VULNERABILITY_EFFECT_NAME = "VulnerabilityEffect";

	public static void applyVulnerability(Plugin plugin, int ticks, double amount, LivingEntity mob) {
		plugin.mEffectManager.addEffect(mob, VULNERABILITY_EFFECT_NAME, new PercentDamageReceived(ticks, amount));
	}

	public static boolean isVulnerable(Plugin plugin, LivingEntity mob) {
		NavigableSet<Effect> vulns = plugin.mEffectManager.getEffects(mob, VULNERABILITY_EFFECT_NAME);
		if (vulns != null) {
			return true;
		}
		return false;
	}

	public static double getVulnAmount(Plugin plugin, LivingEntity mob) {
		NavigableSet<Effect> vulns = plugin.mEffectManager.getEffects(mob, VULNERABILITY_EFFECT_NAME);
		if (vulns != null) {
			Effect vuln = vulns.last();
			return vuln.getMagnitude();
		} else {
			return 0;
		}
	}

	public static int getVulnTicks(Plugin plugin, LivingEntity mob) {
		NavigableSet<Effect> vulns = plugin.mEffectManager.getEffects(mob, VULNERABILITY_EFFECT_NAME);
		if (vulns != null) {
			Effect vuln = vulns.last();
			return vuln.getDuration();
		} else {
			return 0;
		}
	}

	private static final String BLIGHT_EFFECT_NAME = "SanguineHarvestBlightEffect";

	public static boolean isBlighted(Plugin plugin, LivingEntity mob) {
		NavigableSet<Effect> blight = plugin.mEffectManager.getEffects(mob, BLIGHT_EFFECT_NAME);
		if (blight != null) {
			return true;
		}
		return false;
	}

	private static final String BLEED_EFFECT_NAME = "BleedEffect";

	public static void applyBleed(Plugin plugin, int ticks, double amount, LivingEntity mob) {
		plugin.mEffectManager.addEffect(mob, BLEED_EFFECT_NAME, new Bleed(ticks, amount, plugin));
	}

	public static boolean isBleeding(Plugin plugin, LivingEntity mob) {
		return plugin.mEffectManager.hasEffect(mob, BLEED_EFFECT_NAME);
	}

	public static int getBleedLevel(Plugin plugin, LivingEntity mob) {
		NavigableSet<Effect> bleeds = plugin.mEffectManager.getEffects(mob, BLEED_EFFECT_NAME);
		if (bleeds != null) {
			Effect bleed = bleeds.last();
			return (int) bleed.getMagnitude();
		} else {
			return 0;
		}
	}

	public static int getBleedTicks(Plugin plugin, LivingEntity mob) {
		NavigableSet<Effect> bleeds = plugin.mEffectManager.getEffects(mob, BLEED_EFFECT_NAME);
		if (bleeds != null) {
			Effect bleed = bleeds.last();
			return bleed.getDuration();
		} else {
			return 0;
		}
	}

	public static void setBleedTicks(Plugin plugin, LivingEntity mob, int ticks) {
		NavigableSet<Effect> bleeds = plugin.mEffectManager.getEffects(mob, BLEED_EFFECT_NAME);
		if (bleeds != null) {
			Effect bleed = bleeds.last();
			bleed.setDuration(ticks);
		}
	}

	public static final String SLOW_EFFECT_NAME = "SlowEffect";

	public static void applySlow(Plugin plugin, int ticks, double amount, LivingEntity mob) {
		plugin.mEffectManager.addEffect(mob, SLOW_EFFECT_NAME, new PercentSpeed(ticks, -amount, SLOW_EFFECT_NAME));
	}

	public static boolean isSlowed(Plugin plugin, LivingEntity mob) {
		return plugin.mEffectManager.hasEffect(mob, SLOW_EFFECT_NAME);
	}

	public static double getSlowAmount(Plugin plugin, LivingEntity mob) {
		NavigableSet<Effect> slows = plugin.mEffectManager.getEffects(mob, SLOW_EFFECT_NAME);
		if (slows != null) {
			Effect slow = slows.last();
			return slow.getMagnitude();
		} else {
			return 0;
		}
	}

	public static int getSlowTicks(Plugin plugin, LivingEntity mob) {
		NavigableSet<Effect> slows = plugin.mEffectManager.getEffects(mob, SLOW_EFFECT_NAME);
		if (slows != null) {
			Effect slow = slows.last();
			return slow.getDuration();
		} else {
			return 0;
		}
	}

	public static void setSlowTicks(Plugin plugin, LivingEntity mob, int ticks) {
		NavigableSet<Effect> slows = plugin.mEffectManager.getEffects(mob, SLOW_EFFECT_NAME);
		if (slows != null) {
			Effect slow = slows.last();
			slow.setDuration(ticks);
		}
	}

	private static final String WEAKEN_EFFECT_NAME = "WeakenEffect";
	private static final String WEAKEN_EFFECT_AESTHETICS_NAME = "WeakenEffectAesthetics";

	private static final EnumSet<DamageType> WEAKEN_EFFECT_AFFECTED_DAMAGE_TYPES = EnumSet.of(
		DamageType.MELEE,
		DamageType.PROJECTILE
	);

	public static void applyWeaken(Plugin plugin, int ticks, double amount, LivingEntity mob) {
		applyWeaken(plugin, ticks, amount, mob, WEAKEN_EFFECT_AFFECTED_DAMAGE_TYPES);
	}

	public static void applyWeaken(Plugin plugin, int ticks, double amount, LivingEntity mob, @Nullable EnumSet affectedDamageTypes) {
		plugin.mEffectManager.addEffect(mob, WEAKEN_EFFECT_NAME, new PercentDamageDealt(ticks, -amount, affectedDamageTypes));
		plugin.mEffectManager.addEffect(mob, WEAKEN_EFFECT_AESTHETICS_NAME, new Aesthetics(ticks,
			(entity, fourHertz, twoHertz, oneHertz) -> {
				if (fourHertz) {
					if (!(mob instanceof Player p)) {
						return;
					}
					World world = p.getWorld();
					Location rightHand = PlayerUtils.getRightSide(p.getEyeLocation(), 0.45).subtract(0, .8, 0);
					Location leftHand = PlayerUtils.getRightSide(p.getEyeLocation(), -0.45).subtract(0, .8, 0);
					world.spawnParticle(Particle.SMOKE_NORMAL, leftHand, 2, 0.05f, 0.05f, 0.05f, 0);
					world.spawnParticle(Particle.SMOKE_NORMAL, rightHand, 2, 0.05f, 0.05f, 0.05f, 0);
				}
			},
			(entity) -> {

			}));
	}

	public static boolean isWeakened(Plugin plugin, LivingEntity mob) {
		return plugin.mEffectManager.hasEffect(mob, WEAKEN_EFFECT_NAME);
	}

	public static double getWeakenAmount(Plugin plugin, LivingEntity mob) {
		NavigableSet<Effect> weaks = plugin.mEffectManager.getEffects(mob, WEAKEN_EFFECT_NAME);
		if (weaks != null) {
			Effect weak = weaks.last();
			return weak.getMagnitude();
		} else {
			return 0;
		}
	}

	public static int getWeakenTicks(Plugin plugin, LivingEntity mob) {
		NavigableSet<Effect> weaks = plugin.mEffectManager.getEffects(mob, WEAKEN_EFFECT_NAME);
		if (weaks != null) {
			Effect weak = weaks.last();
			return weak.getDuration();
		} else {
			return 0;
		}
	}

	public static void setWeakenTicks(Plugin plugin, LivingEntity mob, int ticks) {
		NavigableSet<Effect> weaks = plugin.mEffectManager.getEffects(mob, WEAKEN_EFFECT_NAME);
		NavigableSet<Effect> weaksAesthetics = plugin.mEffectManager.getEffects(mob, WEAKEN_EFFECT_AESTHETICS_NAME);
		if (weaks != null) {
			Effect weak = weaks.last();
			weak.setDuration(ticks);
		}
		if (weaksAesthetics != null) {
			Effect weak = weaksAesthetics.last();
			weak.setDuration(ticks);
		}
	}

	public static boolean hasDamageOverTime(Plugin plugin, LivingEntity mob) {
		return plugin.mEffectManager.hasEffect(mob, CustomDamageOverTime.class);
	}

	public static int getDamageOverTimeCount(Plugin plugin, LivingEntity mob) {
		return plugin.mEffectManager.getEffects(mob, CustomDamageOverTime.class).size();
	}

	public static double getHighestDamageOverTime(Plugin plugin, LivingEntity mob) {
		double highest = 0;
		for (Effect effect : plugin.mEffectManager.getEffects(mob, CustomDamageOverTime.class)) {
			highest = Math.max(highest, effect.getMagnitude());
		}
		return highest;
	}

	private static void setFireTicksIfLower(int fireTicks, LivingEntity target) {
		if (target.getFireTicks() < fireTicks && !isFireResistant(target)) {
			target.setFireTicks(fireTicks);
		}
	}

	public static void applyFire(Plugin plugin, int fireTicks, LivingEntity target, @Nullable LivingEntity applier) {
		if (applier instanceof Player player) {
			applyFire(plugin, fireTicks, target, player, plugin.mItemStatManager.getPlayerItemStats(player));
		} else if (target instanceof Player player) {
			fireTicks = FireProtection.getFireDuration(fireTicks, plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.FIRE_PROTECTION));
			setFireTicksIfLower(fireTicks, player);
		} else {
			setFireTicksIfLower(fireTicks, target);
		}
	}

	public static void applyFire(Plugin plugin, int fireTicks, LivingEntity target, Player player, @Nullable ItemStatManager.PlayerItemStats playerItemStats) {
		if (target instanceof ArmorStand || target.isInvulnerable()) {
			return;
		}

		int inferno = plugin.mItemStatManager.getEnchantmentLevel(playerItemStats, EnchantmentType.INFERNO);
		if (inferno > 0) {
			Inferno.apply(plugin, player, inferno, target, fireTicks);
		}

		setFireTicksIfLower(fireTicks, target);
	}

	public static void applyTaunt(Plugin plugin, LivingEntity tauntedEntity, Player targetedPlayer) {
		if (!tauntedEntity.getScoreboardTags().contains(IGNORE_TAUNT_TAG)) {
			Mob tauntedMob = (Mob) tauntedEntity;
			tauntedMob.setTarget(targetedPlayer);
			new PartialParticle(Particle.REDSTONE, tauntedEntity.getEyeLocation().add(0, 0.5, 0), 12, 0.4, 0.5, 0.4, TAUNT_COLOR).spawnAsPlayerActive(targetedPlayer);

			// Damage the taunted enemy to keep focus on the player who casted the taunt.
			// Damage bypasses iframes & doesn't affect velocity
			DamageUtils.damage(targetedPlayer, tauntedMob, DamageType.OTHER, 0.001, null, true, false);

		}
	}

	public static boolean isCooling(LivingEntity mob) {
		return COOLING_MOBS.containsKey(mob);
	}

	public static void removeCooling(LivingEntity mob) {
		if (COOLING_MOBS.containsKey(mob)) {
			COOLING_MOBS.put(mob, 0);
		}
	}

	// Used when a mob is rendered immobile as a result of its own actions, e.g. TP-Behind; behaves similarly to stun
	public static void applyCooling(Plugin plugin, int ticks, LivingEntity mob) {
		if (mobsTracker == null || mobsTracker.isCancelled()) {
			startTracker(plugin);
		}

		if (mob instanceof Mob) {
			((Mob) mob).setTarget(null);
		}

		// Only reduce speed if mob is not already in map. We can avoid storing original speed by just +/- 10.
		Integer t = COOLING_MOBS.get(mob);
		if (t == null) {
			AttributeInstance speed = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
			AttributeModifier mod = new AttributeModifier(COOLING_ATTR_NAME, -10, AttributeModifier.Operation.ADD_NUMBER);
			speed.addModifier(mod);
		}
		if (t == null || t < ticks) {
			COOLING_MOBS.put(mob, ticks);
		}
	}

	public static boolean isStunned(Entity mob) {
		return STUNNED_MOBS.containsKey(mob);
	}

	public static void removeStun(LivingEntity mob) {
		STUNNED_MOBS.put(mob, 0);
	}

	public static void applyStun(Plugin plugin, int ticks, LivingEntity mob) {
		if (isBoss(mob) || mob.getScoreboardTags().contains(CrowdControlImmunityBoss.identityTag)) {
			return;
		}

		if (mobsTracker == null || mobsTracker.isCancelled()) {
			startTracker(plugin);
		}

		if (mob instanceof Mob m) {
			m.setTarget(null);
		}

		if (MetadataUtils.checkOnceThisTick(plugin, mob, "StunnedThisTick")) {
			/* Fake "event" so bosses can handle being stunned if they need to */
			BossManager.getInstance().entityStunned(mob);
		}

		// Only reduce speed if mob is not already in map. We can avoid storing original speed by just +/- 10.
		Integer t = STUNNED_MOBS.get(mob);
		if (t == null) {
			AttributeInstance speed = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
			AttributeModifier mod = new AttributeModifier(STUN_ATTR_NAME, -10, AttributeModifier.Operation.ADD_NUMBER);
			speed.addModifier(mod);
			if (mob instanceof Mob) {
				NmsUtils.getVersionAdapter().cancelStrafe((Mob) mob);
			}
		}
		if (t == null || t < ticks) {
			STUNNED_MOBS.put(mob, ticks);
		}
	}

	public static final String NO_RECOIL_EFFECT_NAME = "DisableRecoilMidair";

	public static void applyRecoilDisable(Plugin plugin, int ticks, int amount, LivingEntity mob) {
		plugin.mEffectManager.addEffect(mob, NO_RECOIL_EFFECT_NAME, new RecoilDisable(ticks, amount));
	}

	public static double getRecoilDisableAmount(Plugin plugin, LivingEntity mob) {
		NavigableSet<Effect> disable = plugin.mEffectManager.getEffects(mob, NO_RECOIL_EFFECT_NAME);
		if (disable != null) {
			Effect d = disable.last();
			return d.getMagnitude();
		} else {
			return 0;
		}
	}

	public static boolean isRecoilDisable(Plugin plugin, LivingEntity mob, int amount) {
		if (getRecoilDisableAmount(plugin, mob) >= amount) {
			return true;
		}
		return false;
	}

	public static boolean isRiptideDisable(Plugin plugin, LivingEntity mob) {
		NavigableSet<Effect> disable = plugin.mEffectManager.getEffects(mob, NO_RECOIL_EFFECT_NAME);
		if (disable != null) {
			return true;
		} else {
			return false;
		}
	}

	private static final String ARROW_IFRAMES_EFFECT_NAME = "SplitArrrowIframesEffect";

	public static void applyArrowIframes(Plugin plugin, int ticks, LivingEntity mob) {
		plugin.mEffectManager.addEffect(mob, ARROW_IFRAMES_EFFECT_NAME, new SplitArrowIframesEffect(ticks));
	}

	public static boolean hasArrowIframes(Plugin plugin, LivingEntity mob) {
		return plugin.mEffectManager.hasEffect(mob, ARROW_IFRAMES_EFFECT_NAME);
	}

	public static boolean isSilenced(Entity mob) {
		return SILENCED_MOBS.containsKey(mob);
	}

	public static void removeSilence(LivingEntity mob) {
		SILENCED_MOBS.put(mob, 0);
	}

	public static void applySilence(Plugin plugin, int ticks, LivingEntity mob) {
		if (isBoss(mob)) {
			return;
		}

		if (mobsTracker == null || mobsTracker.isCancelled()) {
			startTracker(plugin);
		}

		/* Fake "event" so bosses can handle being silenced if they need to */
		BossManager.getInstance().entitySilenced(mob);

		Integer t = SILENCED_MOBS.get(mob);
		if (t == null || t < ticks) {
			SILENCED_MOBS.put(mob, ticks);
		}
	}

	public static void summonEntityAt(Location loc, EntityType type, String nbt) {
		try {
			getSummonEntityAt(loc, type, nbt);
		} catch (Exception ex) {
			Plugin.getInstance().getLogger().warning("Attempted to summon entity " + type.toString() + " but no entity appeared");
		}
	}

	/*
	 * TODO: This is really janky - it *probably* returns the correct entity... but it might not
	 */
	public static Entity getSummonEntityAt(Location loc, EntityType type, String nbt) throws Exception {
		String worldName = Bukkit.getWorlds().get(0).equals(loc.getWorld()) ? "overworld" : loc.getWorld().getName();
		String cmd = "execute in " + worldName + " run summon " + type.getName() + " " + loc.getX() + " " + loc.getY() + " " + loc.getZ() + " " + nbt;
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);

		return loc.getNearbyEntities(1f, 1f, 1f)
			.stream()
			.filter(e -> e.getType().equals(type))
			.min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(loc)))
			.orElseThrow(() -> new Exception("Summoned mob but no mob appeared - " + cmd));
	}

	/*
	 * When we retrieve the location of the projectile, we get the location of the projectile the tick before
	 * it hits; any location data retrieved from later ticks is unreliable. This relies on the fact that the
	 * location on the tick before the actual hit is close to the location of the actual hit.
	 */
	public static Location getProjectileHitLocation(ProjectileHitEvent event) {
		Projectile proj = event.getEntity();
		World world = proj.getWorld();
		BoundingBox hitbox = proj.getBoundingBox();
		Vector increment = proj.getVelocity();
		int increments = (int) (increment.length() * 15);
		increment.normalize().multiply(0.1);

		Block block = event.getHitBlock();
		BoundingBox target = block != null ? block.getBoundingBox() : event.getHitEntity().getBoundingBox();

		for (int i = 0; i < increments; i++) {
			hitbox.shift(increment);
			if (hitbox.overlaps(target)) {
				return hitbox.getCenter().add(increment).toLocation(world);
			}
		}

		// If our manual search didn't find the target, then just default to the buggy location value
		return proj.getLocation();
	}

	// Only use this to set max health of newly spawned mobs
	public static void scaleMaxHealth(LivingEntity mob, double modifierPercent, String modifierName) {
		// Make sure the mob never has an invalid health
		if (modifierPercent < 0) {
			mob.setHealth(mob.getHealth() * (1 + modifierPercent));
		}

		addAttribute(mob, Attribute.GENERIC_MAX_HEALTH,
			new AttributeModifier(modifierName, modifierPercent, Operation.MULTIPLY_SCALAR_1));
		mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
	}

	public static void addAttribute(Attributable attributable, Attribute attribute, AttributeModifier modifier) {
		AttributeInstance instance = attributable.getAttribute(attribute);
		if (instance != null) {
			instance.addModifier(modifier);
		}
	}

	public static void removeAttribute(Attributable attributable, Attribute attribute, String modifierName) {
		AttributeInstance instance = attributable.getAttribute(attribute);
		if (instance != null) {
			for (AttributeModifier modifier : instance.getModifiers()) {
				if (modifier != null && modifier.getName().equals(modifierName)) {
					instance.removeModifier(modifier);
				}
			}
		}
	}

	public static void removeAttributesContaining(Attributable attributable, Attribute attribute, String modifierName) {
		AttributeInstance instance = attributable.getAttribute(attribute);
		if (instance != null) {
			for (AttributeModifier modifier : instance.getModifiers()) {
				if (modifier != null && modifier.getName().contains(modifierName)) {
					instance.removeModifier(modifier);
				}
			}
		}
	}

	public static boolean hasAttributesContaining(Attributable attributable, Attribute attribute, String modifierName) {
		AttributeInstance instance = attributable.getAttribute(attribute);
		if (instance != null) {
			for (AttributeModifier modifier : instance.getModifiers()) {
				if (modifier != null && modifier.getName().contains(modifierName)) {
					return true;
				}
			}
		}
		return false;
	}


	public static double getMaxHealth(LivingEntity entity) {
		AttributeInstance maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
		return maxHealth == null ? 0 : maxHealth.getValue();
	}

	/**
	 * Returns {@code entity.getAttribute(attribute).getValue(value)} if the attribute exists, or {@code def} if not.
	 */
	public static double getAttributeOrDefault(LivingEntity entity, Attribute attribute, double def) {
		AttributeInstance attr = entity.getAttribute(attribute);
		return attr == null ? def : attr.getValue();
	}

	/**
	 * Returns {@code entity.getAttribute(attribute).getBaseValue(value)} if the attribute exists, or {@code def} if not.
	 */
	public static double getAttributeBaseOrDefault(LivingEntity entity, Attribute attribute, double def) {
		AttributeInstance attr = entity.getAttribute(attribute);
		return attr == null ? def : attr.getBaseValue();
	}

	/**
	 * Null-safe version of {@code entity.getAttribute(attribute).setBaseValue(value)}
	 */
	public static void setAttributeBase(LivingEntity entity, Attribute attribute, double value) {
		AttributeInstance attr = entity.getAttribute(attribute);
		if (attr != null) {
			attr.setBaseValue(value);
		}
	}

	public static boolean isSomeArrow(Entity projectile) {
		return isSomeArrow(projectile.getType());
	}

	public static boolean isSomeArrow(EntityType entityType) {
		// TippedArrow is deprecated
		return entityType == EntityType.ARROW || entityType == EntityType.SPECTRAL_ARROW;
	}

	private static final String PARALYZE_EFFECT_NAME = "ParalyzeEffect";

	public static void paralyze(Plugin plugin, int ticks, LivingEntity mob) {
		plugin.mEffectManager.addEffect(mob, PARALYZE_EFFECT_NAME, new Paralyze(ticks, plugin));
	}

	public static boolean isParalyzed(Plugin plugin, LivingEntity mob) {
		return plugin.mEffectManager.hasEffect(mob, PARALYZE_EFFECT_NAME);
	}

	public static void removeParalysis(Plugin plugin, LivingEntity mob) {
		NavigableSet<Effect> paralyses = plugin.mEffectManager.getEffects(mob, PARALYZE_EFFECT_NAME);
		if (paralyses != null) {
			paralyses.last().setDuration(0);
		}
	}

	public static double calculateCreeperExplosionDamage(Creeper creeper, LivingEntity entity, double originalDamage) {
		double baseDamage = getAttributeBaseOrDefault(creeper, Attribute.GENERIC_ATTACK_DAMAGE, 0);

		//Vanilla creepers have 2 attack damage for some reason - if we haven't intentionally set it, don't change the damage
		if (baseDamage <= 2) {
			return originalDamage;
		}

		double power = creeper.getExplosionRadius();
		if (creeper.isPowered()) {
			power *= 2;
		}

		return getAdjustedBlastDamage(power, originalDamage, baseDamage);
	}

	private static double getAdjustedBlastDamage(double power, double originalDamage, double baseDamage) {
		//Vanilla formula for maximum damage taken
		double maxOriginalDamage = 2 * 7 * power + 1;

		//1 damage is constant and doesn't scale
		double ratio = (originalDamage - 1) / (maxOriginalDamage - 1);
		return ratio * (baseDamage - 1) + 1;
	}

	public static boolean isTrainingDummy(LivingEntity e) {
		Set<String> tags = e.getScoreboardTags();
		return tags.contains("boss_training_dummy");
	}

	// Adds a Tag which Removes the entity on unload.
	// See EntityListener, EntityRemoveFromWorldEvent
	public static void setRemoveEntityOnUnload(Entity e) {
		e.getScoreboardTags().add("REMOVE_ON_UNLOAD");
	}

	public static float getCounterclockwiseAngle(Entity e1, Entity e2) {
		Vector loc1 = e1.getLocation().toVector();
		Vector loc2 = e2.getLocation().toVector();
		Vector lineOfSight = loc2.clone().subtract(loc1);
		lineOfSight.setY(0).normalize();
		// Treat it as a giant unit circle with axes (z, -x)
		double angleCounterclockwise = Math.acos(lineOfSight.getZ());
		if (lineOfSight.getX() > 0) {
			angleCounterclockwise = -angleCounterclockwise;
		}
		return (float)angleCounterclockwise;
	}
}
