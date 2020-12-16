package com.playmonumenta.plugins.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Giant;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.PolarBear;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.PufferFish;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Rabbit.Type;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.Slime;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.ZombieHorse;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.CrowdControlImmunityBoss;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.enchantments.Inferno;
import com.playmonumenta.plugins.events.CustomDamageEvent;

public class EntityUtils {

	private static final EnumSet<DamageCause> PHYSICAL_DAMAGE = EnumSet.of(
			DamageCause.CUSTOM,
			DamageCause.ENTITY_ATTACK,
			DamageCause.ENTITY_EXPLOSION,
			DamageCause.ENTITY_SWEEP_ATTACK,
			DamageCause.FALLING_BLOCK,
			DamageCause.FIRE,
			DamageCause.HOT_FLOOR,
			DamageCause.LAVA,
			DamageCause.LIGHTNING,
			DamageCause.PROJECTILE,
			DamageCause.THORNS
	);

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

	private static final EnumSet<EntityType> ARTHROPOD_MOBS = EnumSet.of(
			EntityType.SPIDER,
			EntityType.CAVE_SPIDER,
			EntityType.SILVERFISH,
			EntityType.ENDERMITE,
			EntityType.BEE
	);

	private static final EnumSet<EntityType> AQUATIC_MOBS = EnumSet.of(
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

	private static final EnumSet<EntityType> BEAST_MOBS = EnumSet.of(
			EntityType.CREEPER,
			EntityType.BLAZE,
			EntityType.GHAST,
			EntityType.ENDERMAN,
			EntityType.ENDER_DRAGON,
			EntityType.WOLF,
			EntityType.OCELOT,
			EntityType.RAVAGER,
			EntityType.SLIME,
			EntityType.MAGMA_CUBE,
			EntityType.SHULKER,
			EntityType.POLAR_BEAR,
			EntityType.BAT,
			EntityType.CAT,
			EntityType.CHICKEN,
			EntityType.COW,
			EntityType.DONKEY,
			EntityType.FOX,
			EntityType.HORSE,
			EntityType.LLAMA,
			EntityType.MULE,
			EntityType.MUSHROOM_COW,
			EntityType.PANDA,
			EntityType.PARROT,
			EntityType.PIG,
			EntityType.RABBIT,
			EntityType.SHEEP
	);

	private static final EnumSet<EntityType> HUMANOID_MOBS = EnumSet.of(
			EntityType.VINDICATOR,
			EntityType.ILLUSIONER,
			EntityType.EVOKER,
			EntityType.VEX,
			EntityType.WITCH,
			EntityType.IRON_GOLEM,
			EntityType.GIANT,
			EntityType.PILLAGER,
			EntityType.SNOWMAN
	);

	private static final Map<LivingEntity, Integer> COOLING_MOBS = new HashMap<LivingEntity, Integer>();
	private static final Map<LivingEntity, Integer> STUNNED_MOBS = new HashMap<LivingEntity, Integer>();
	private static final Map<LivingEntity, Integer> CONFUSED_MOBS = new HashMap<LivingEntity, Integer>();
	private static BukkitRunnable mobsTracker = null;

	private static final Particle.DustOptions STUN_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 100), 1.0f);
	private static final Particle.DustOptions CONFUSION_COLOR = new Particle.DustOptions(Color.fromRGB(62, 0, 102), 1.0f);

	private static void startTracker(Plugin plugin) {
		mobsTracker = new BukkitRunnable() {
			int mRotation = 0;

			@Override
			public void run() {
				mRotation += 20;

				Iterator<Map.Entry<LivingEntity, Integer>> coolingIter = COOLING_MOBS.entrySet().iterator();
				Iterator<Map.Entry<LivingEntity, Integer>> stunnedIter = STUNNED_MOBS.entrySet().iterator();
				Iterator<Map.Entry<LivingEntity, Integer>> confusedIter = CONFUSED_MOBS.entrySet().iterator();

				while (coolingIter.hasNext()) {
					Map.Entry<LivingEntity, Integer> cooling = coolingIter.next();
					LivingEntity mob = cooling.getKey();
					COOLING_MOBS.put(mob, cooling.getValue() - 1);

					if (cooling.getValue() <= 0 || mob.isDead() || !mob.isValid()) {
						mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue() + 10);

						if (mob instanceof Mob) {
							((Mob) mob).setTarget(getNearestPlayer(mob.getLocation(), mob.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).getValue()));
						}

						coolingIter.remove();
					}
				}

				while (stunnedIter.hasNext()) {
					Map.Entry<LivingEntity, Integer> stunned = stunnedIter.next();
					LivingEntity mob = stunned.getKey();
					STUNNED_MOBS.put(mob, stunned.getValue() - 1);

					double angle = Math.toRadians(mRotation);
					Location l = mob.getLocation();
					l.add(FastUtils.cos(angle) * 0.5, mob.getHeight(), FastUtils.sin(angle) * 0.5);
					mob.getWorld().spawnParticle(Particle.REDSTONE, l, 5, 0, 0, 0, STUN_COLOR);

					if (stunned.getValue() <= 0 || mob.isDead() || !mob.isValid()) {
						mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue() + 10);
						stunnedIter.remove();
					}
				}

				while (confusedIter.hasNext()) {
					Map.Entry<LivingEntity, Integer> confused = confusedIter.next();
					Mob mob = (Mob) confused.getKey();
					CONFUSED_MOBS.put(mob, confused.getValue() - 1);

					double angle = Math.toRadians(mRotation);
					Location l = mob.getLocation();
					l.add(FastUtils.cos(angle) * 0.5, mob.getHeight() + 0.25, FastUtils.sin(angle) * 0.5);
					mob.getWorld().spawnParticle(Particle.REDSTONE, l, 2, 0, 0, 0, CONFUSION_COLOR);

					if (mob.getTarget() == null) {
						mob.setTarget(getNearestMob(mob.getLocation(), 8));
					}

					if (confused.getValue() <= 0 || mob.isDead() || !mob.isValid()) {
						mob.setTarget(null);
						confusedIter.remove();
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

	// Affected by Bane of Arthropods
	public static boolean isArthropod(LivingEntity mob) {
		return ARTHROPOD_MOBS.contains(mob.getType());
	}

	// Affected by Impaling
	public static boolean isAquatic(LivingEntity mob) {
		return AQUATIC_MOBS.contains(mob.getType());
	}

	// Affected by Slayer
	public static boolean isBeast(LivingEntity mob) {
		return BEAST_MOBS.contains(mob.getType());
	}

	// Affected by Duelist
	public static boolean isHumanoid(LivingEntity mob) {
		return HUMANOID_MOBS.contains(mob.getType());
	}

	public static boolean isElite(Entity entity) {
		Set<String> tags = entity.getScoreboardTags();
		return tags.contains("Elite");
	}

	public static boolean isBoss(Entity entity) {
		Set<String> tags = entity.getScoreboardTags();
		return tags.contains("Boss");
	}

	public static boolean isHostileMob(Entity entity) {
		if (!entity.getScoreboardTags().contains("SkillImmune")) {
			if (entity instanceof Monster || entity instanceof Slime || entity instanceof Ghast || entity instanceof PolarBear
					|| entity instanceof Phantom || entity instanceof Shulker || entity instanceof PufferFish
					|| entity instanceof SkeletonHorse || entity instanceof ZombieHorse || entity instanceof Giant) {
				return true;
			} else if (entity instanceof Wolf) {
				return ((Wolf) entity).isAngry() || entity.getScoreboardTags().contains("boss_targetplayer");
			} else if (entity instanceof Rabbit) {
				return ((Rabbit) entity).getRabbitType() == Type.THE_KILLER_BUNNY;
			} else if (entity instanceof Player) {
				return AbilityManager.getManager().isPvPEnabled((Player) entity);
			} else if (entity instanceof Mob) {
				LivingEntity target = ((Mob) entity).getTarget();
				return (target != null && target instanceof Player) || entity.getScoreboardTags().contains("boss_targetplayer");
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

		for (Entity ne : loc.getWorld().getNearbyEntities(loc, 0.75, 0.75, 0.75)) {
			if (ne.getUniqueId().equals(entity.getUniqueId())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Gets the entity in the crosshair of the player
	 * <p>
	 * Utilizes raycasting for the detection.
	 * @param player
	 * @param range
	 * @param targetPlayers
	 * @param targetNonPlayers
	 * @return The Entity in the crosshair of the player
	 */
	public static LivingEntity getCrosshairTarget(Player player, int range, boolean targetPlayers,
	                                              boolean targetNonPlayers, boolean checkLos, boolean throughNonOccluding) {
		Location loc = player.getEyeLocation();
		Vector dir = loc.getDirection();

		for (int i = 0; i < (range * 2); i++) {
			loc.add(dir.clone().multiply(0.5));
			//Is the block solid?

			if (checkLos) {
				if (loc.getBlock().getType().isSolid()) {
					if (throughNonOccluding) {
						if (loc.getBlock().getType().isOccluding()) {
							return null;
						}
					} else {
						return null;
					}
				}
			}
			for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.75, 0.75, 0.75)) {
				//  Make sure to only get living entities.
				if (e instanceof LivingEntity) {
					//  Make sure we should be targeting this entity.
					if ((targetPlayers && (e instanceof Player)) || (targetNonPlayers && (!(e instanceof Player)))) {
						return (LivingEntity)e;
					}
				}
			}
		}

		return null;
	}

	public static LivingEntity getEntityAtCursor(Player player, int range, boolean targetPlayers, boolean targetNonPlayers, boolean checkLos) {
		List<Entity> en = player.getNearbyEntities(range, range, range);
		ArrayList<LivingEntity> entities = new ArrayList<LivingEntity>();
		for (Entity e : en) {
			//  Make sure to only get living entities.
			if (e instanceof LivingEntity) {
				//  Make sure we should be targeting this entity.
				if ((targetPlayers && (e instanceof Player)) || (targetNonPlayers && (!(e instanceof Player)))) {
					entities.add((LivingEntity)e);
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

			//  If we want to check Line of sight we want to make sure the the blocks are transparent.
			if (checkLos && LocationUtils.isLosBlockingBlock(b.getType())) {
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

	public static Projectile spawnArrow(Plugin plugin, Player player, Vector rotation, Vector offset, Vector speed, Class<? extends AbstractArrow> arrowClass) {
		Location loc = player.getEyeLocation();
		loc.add(offset);
		loc.setPitch(loc.getPitch() + (float)rotation.getX());
		loc.setYaw(loc.getYaw() + (float)rotation.getY());
		Vector vel = new Vector(loc.getDirection().getX() * speed.getX(), loc.getDirection().getY() * speed.getY(), loc.getDirection().getZ() * speed.getZ());

		World world = player.getWorld();
		AbstractArrow arrow = world.spawnArrow(loc, vel, 0.6f, 12.0f, arrowClass);

		arrow.setShooter(player);
		arrow.setVelocity(vel);

		return arrow;
	}

	public static List<Projectile> spawnArrowVolley(Plugin plugin, Player player, int numProjectiles, double speedModifier, double spacing, Class<? extends AbstractArrow> arrowClass) {
		List<Projectile> projectiles = new ArrayList<Projectile>();

		Vector speed = new Vector(1.75 * speedModifier, 2 * speedModifier, 1.75 * speedModifier);

		for (double yaw = -spacing * (numProjectiles / 2); yaw < spacing * ((numProjectiles / 2) + 1); yaw += spacing) {
			Projectile proj = spawnArrow(plugin, player, new Vector(0, yaw, 0), new Vector(0, 0, 0), speed, arrowClass);
			if (proj != null) {
				projectiles.add(proj);
			}
		}

		return projectiles;
	}

	public static AreaEffectCloud spawnAreaEffectCloud(World world, Location loc, Collection<PotionEffect> effects, float radius, int duration) {
		AreaEffectCloud cloud = (AreaEffectCloud)world.spawnEntity(loc, EntityType.AREA_EFFECT_CLOUD);

		for (PotionEffect effect : effects) {
			cloud.addCustomEffect(effect, false);
		}

		cloud.setRadius(radius);
		cloud.setDuration(duration);

		return cloud;
	}

	public static ThrownPotion spawnCustomSplashPotion(Player player, ItemStack potionStack, Location loc) {
		ThrownPotion potion = (ThrownPotion)loc.getWorld().spawnEntity(loc.add(0, 0.5, 0), EntityType.SPLASH_POTION);
		potion.setShooter(player);
		potion.setItem(potionStack);

		return potion;
	}

	public static boolean withinRangeOfMonster(Player player, double range) {
		List<Entity> entities = player.getNearbyEntities(range, range, range);
		for (Entity entity : entities) {
			if (isHostileMob(entity)) {
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
	 * @return      List of LivingEntity objects of the specified type within the bounding box
	 */
	public static List<LivingEntity> getNearbyMobs(Location loc, double rx, double ry, double rz, EnumSet<EntityType> types) {
		Collection<Entity> entities = loc.getWorld().getNearbyEntities(loc, rx, ry, rz);

		List<LivingEntity> mobs = new ArrayList<LivingEntity>(entities.size());
		for (Entity entity : entities) {
			if (types == null) {
				if (types == null && isHostileMob(entity) && !entity.isDead() && entity.isValid()) {
					mobs.add((LivingEntity)entity);
				}
			} else {
				if (types.contains(entity.getType()) && !entity.isDead() && entity.isValid()) {
					mobs.add((LivingEntity)entity);
				}
			}
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

	public static List<LivingEntity> getNearbyMobs(Location loc, double radius) {
		return getNearbyMobs(loc, radius, radius, radius);
	}

	public static List<LivingEntity> getNearbyMobs(Location loc, double radius, EnumSet<EntityType> types) {
		return getNearbyMobs(loc, radius, radius, radius, types);
	}

	public static List<LivingEntity> getMobsInLine(Location loc, Vector direction, double range, double halfHitboxLength) {
		Set<LivingEntity> nearbyMobs = new HashSet<LivingEntity>(getNearbyMobs(loc, range));
		List<LivingEntity> mobsInLine = new ArrayList<LivingEntity>();

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

	public static LivingEntity getNearestMob(Location loc, double radius, LivingEntity getter) {
		return getNearestMob(loc, getNearbyMobs(loc, radius, getter));
	}

	public static LivingEntity getNearestMob(Location loc, double radius) {
		return getNearestMob(loc, getNearbyMobs(loc, radius));
	}

	public static LivingEntity getNearestMob(Location loc, List<LivingEntity> nearbyMobs) {
		LivingEntity nearest = null;
		double nearestDistanceSquared = Double.POSITIVE_INFINITY;

		for (LivingEntity mob : nearbyMobs) {
			double distanceSquared = mob.getLocation().distanceSquared(loc);
			if (distanceSquared < nearestDistanceSquared) {
				nearest = mob;
				nearestDistanceSquared = distanceSquared;
			}
		}

		return nearest;
	}

	public static Player getNearestPlayer(Location loc, double radius) {
		List<Player> nearbyPlayers = PlayerUtils.playersInRange(loc, radius);
		if (nearbyPlayers.size() == 0) {
			return null;
		}

		nearbyPlayers.sort((e1, e2) -> e1.getLocation().distance(loc) <= e2.getLocation().distance(loc) ? 1 : -1);
		return nearbyPlayers.get(0);
	}

	// Gets players within radius of the location and sorts them by distance
	// WARNING: Distance is sorted from furthest to closest
	// i.e. The player 20 blocks away is closer to the front of the list than the player 10 blocks away
	// If you want to find the closest players, use the end of the list, the farthest, use the beginning
	public static List<Player> getNearestPlayers(Location loc, double radius) {
		List<Player> nearbyPlayers = PlayerUtils.playersInRange(loc, radius);
		nearbyPlayers.sort((e1, e2) -> e1.getLocation().distance(loc) <= e2.getLocation().distance(loc) ? 1 : -1);
		return nearbyPlayers;
	}

	public static Entity getEntity(World world, UUID entityUUID) {
		List<Entity> entities = world.getEntities();
		for (Entity entity : entities) {
			if (entity.getUniqueId() == entityUUID) {
				return entity;
			}
		}

		return null;
	}

	// Manually calculates the real final damage dealt to a player, in case of manual event calls or absorption hearts.
	// Evasion is not accounted for, as evasion modifies the actual event damage. Works only for players.
	public static double getRealFinalDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			// Give garbage value if being used incorrectly
			return -1;
		}

		Player player = (Player) event.getEntity();
		DamageCause cause = event.getCause();
		double damage = event.getDamage();
		double armor = 0;
		double toughness = 0;

		if (PHYSICAL_DAMAGE.contains(cause)) {
			armor = player.getAttribute(Attribute.GENERIC_ARMOR).getValue();
			toughness = player.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).getValue();
		}

		int protection = 0;
		ItemStack[] armorContents = player.getInventory().getArmorContents();

		for (int i = 0; i < armorContents.length; i++) {
			if (armorContents[i] != null) {
				if (armorContents[i].containsEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL)) {
					protection += armorContents[i].getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL);
				}

				if (cause == DamageCause.PROJECTILE) {
					if (armorContents[i].containsEnchantment(Enchantment.PROTECTION_PROJECTILE)) {
						protection += armorContents[i].getEnchantmentLevel(Enchantment.PROTECTION_PROJECTILE) * 2;
					}
				} else if (cause == DamageCause.BLOCK_EXPLOSION || cause == DamageCause.ENTITY_EXPLOSION) {
					if (armorContents[i].containsEnchantment(Enchantment.PROTECTION_EXPLOSIONS)) {
						protection += armorContents[i].getEnchantmentLevel(Enchantment.PROTECTION_EXPLOSIONS) * 2;
					}
				} else if (cause == DamageCause.FIRE || cause == DamageCause.FIRE_TICK || cause == DamageCause.HOT_FLOOR || cause == DamageCause.LAVA) {
					if (armorContents[i].containsEnchantment(Enchantment.PROTECTION_FIRE)) {
						protection += armorContents[i].getEnchantmentLevel(Enchantment.PROTECTION_FIRE) * 2;
					}
				} else if (cause == DamageCause.FALL) {
					if (armorContents[i].containsEnchantment(Enchantment.PROTECTION_FALL)) {
						protection += armorContents[i].getEnchantmentLevel(Enchantment.PROTECTION_FALL) * 3;
					}
				}
			}
		}

		int resistance = player.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE) == null
				? 0 : (player.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE).getAmplifier() + 1);
		int vulnerability = (player.getPotionEffect(PotionEffectType.UNLUCK) == null
				? 0 : (player.getPotionEffect(PotionEffectType.UNLUCK).getAmplifier() + 1));

		return calculateDamageAfterArmor(damage, armor, toughness) * (1 - Math.min(20.0, protection) / 25) * (1 - Math.min(5, resistance) / 5) * (1 + 0.05 * vulnerability);
	}

	public static void damageEntity(Plugin plugin, LivingEntity target, double damage, Entity damager) {
		damageEntity(plugin, target, damage, damager, null);
	}

	public static void damageEntity(Plugin plugin, LivingEntity target, double damage, Entity damager, MagicType magicType) {
		damageEntity(plugin, target, damage, damager, magicType, true);
	}

	public static void damageEntity(Plugin plugin, LivingEntity target, double damage, Entity damager, MagicType magicType, boolean callEvent) {
		damageEntity(plugin, target, damage, damager, magicType, callEvent, null);
	}

	public static void damageEntity(Plugin plugin, LivingEntity target, double damage, Entity damager, MagicType magicType, boolean callEvent, Spells spell) {
		damageEntity(plugin, target, damage, damager, magicType, callEvent, spell, true, true);
	}

	public static void damageEntity(Plugin plugin, LivingEntity target, double damage, Entity damager, MagicType magicType, boolean callEvent, Spells spell, boolean applySpellshock, boolean triggerSpellshock) {
		if (!target.isDead() && !target.isInvulnerable()) {
			CustomDamageEvent event = new CustomDamageEvent(damager, target, damage, magicType, callEvent, spell, applySpellshock, triggerSpellshock);
			Bukkit.getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return;
			}
			damage = event.getDamage();

			if (target.getNoDamageTicks() == target.getMaximumNoDamageTicks()) {
				target.setNoDamageTicks(0);
			}

			if (!(damager instanceof Player)) {
				// Applies DamageCause.ENTITY_ATTACK
				target.damage(damage, damager);
			} else if (damager instanceof Player) {
				if (magicType != null && !magicType.equals(MagicType.PHYSICAL) && !magicType.equals(MagicType.NONE)) {
					MetadataUtils.checkOnceThisTick(plugin, damager, "LastMagicUseTime");
				}
				// Applies DamageCause.CUSTOM
				NmsUtils.customDamageEntity(target, damage, (Player) damager, "magic");
			} else {
				// Applies DamageCause.GENERIC
				target.damage(damage);
			}
		}
	}

	public static double vulnerabilityMult(LivingEntity target) {
		PotionEffect unluck = target.getPotionEffect(PotionEffectType.UNLUCK);
		if (unluck != null) {
			double vulnLevel = 1 + unluck.getAmplifier();

			if (EntityUtils.isBoss(target)) {
				vulnLevel = vulnLevel / 2;
			}

			return 1 + 0.05 * vulnLevel;
		}

		return 1;
	}

	public static LivingEntity getNearestHostile(Player player, double range) {
		LivingEntity target = null;
		double maxDist = range;

		for (Entity e : player.getNearbyEntities(range, range, range)) {
			if (isHostileMob(e) && !e.isDead()) {
				LivingEntity le = (LivingEntity) e;

				if (le.getLocation().distance(player.getLocation()) < maxDist) {
					maxDist = le.getLocation().distance(player.getLocation());
					target = le;
				}
			}
		}
		return target;
	}

	private static final String VULNERABILITY_EFFECT_NAME = "VulnerabilityEffect";

	public static void applyVulnerability(Plugin plugin, int ticks, double amount, LivingEntity mob) {
		plugin.mEffectManager.addEffect(mob, VULNERABILITY_EFFECT_NAME, new PercentDamageReceived(ticks, amount));
	}

	private static final String WEAKEN_EFFECT_NAME = "WeakenEffect";
	private static final EnumSet<DamageCause> WEAKEN_EFFECT_AFFECTED_DAMAGE_CAUSES = EnumSet.of(
			DamageCause.ENTITY_ATTACK,
			DamageCause.PROJECTILE
	);

	public static void applyWeaken(Plugin plugin, int ticks, double amount, LivingEntity mob) {
		plugin.mEffectManager.addEffect(mob, WEAKEN_EFFECT_NAME, new PercentDamageDealt(ticks, -amount, WEAKEN_EFFECT_AFFECTED_DAMAGE_CAUSES));
	}

	public static void applyFire(Plugin plugin, int ticks, LivingEntity mob, Player player) {
		mob.setMetadata(Inferno.SET_FIRE_TICK_METAKEY, new FixedMetadataValue(plugin, mob.getTicksLived()));
		mob.setMetadata(Inferno.FIRE_TICK_METAKEY, new FixedMetadataValue(plugin, mob.getTicksLived()));
		mob.setFireTicks(ticks);

		// Inferno detects a damage event (since it can light fire resistant mobs "on fire"), so do some damage
		// We need to do this even for abilities that deal damage, since the damage events might not occur due to iFrames
		int iFrames = mob.getNoDamageTicks();
		Vector velocity = mob.getVelocity();
		mob.setNoDamageTicks(0);
		EntityUtils.damageEntity(plugin, mob, 0.001, player, MagicType.FIRE, false, null, false, false);
		mob.setNoDamageTicks(iFrames);
		mob.setVelocity(velocity);
	}

	public static boolean isCooling(Entity mob) {
		return COOLING_MOBS.containsKey(mob);
	}

	public static void removeCooling(LivingEntity mob) {
		COOLING_MOBS.put(mob, 0);
	}

	// Used when a mob is rendered immobile as a result of its own actions, e.g. TP-Behind; behaves similarly to stun
	public static void applyCooling(Plugin plugin, int ticks, LivingEntity mob) {
		if (mobsTracker == null || mobsTracker.isCancelled()) {
			startTracker(plugin);
		}

		if (mob instanceof Mob) {
			((Mob) mob).setTarget(null);
		}

		if (mob instanceof Vex) {
			mob.setVelocity(new Vector(0, 0, 0));
		}

		// Only reduce speed if mob is not already in map. We can avoid storing original speed by just +/- 10.
		Integer t = COOLING_MOBS.get(mob);
		if (t == null) {
			mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue() - 10);
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

		if (mob instanceof Mob) {
			((Mob) mob).setTarget(null);
		}

		if (mob instanceof Vex) {
			mob.setVelocity(new Vector(0, 0, 0));
		}

		/* Fake "event" so bosses can handle being stunned if they need to */
		BossManager.getInstance().entityStunned(mob);

		// Only reduce speed if mob is not already in map. We can avoid storing original speed by just +/- 10.
		Integer t = STUNNED_MOBS.get(mob);
		if (t == null) {
			mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue() - 10);
		}
		if (t == null || t < ticks) {
			STUNNED_MOBS.put(mob, ticks);
		}
	}

	public static boolean isConfused(Entity mob) {
		return CONFUSED_MOBS.containsKey(mob);
	}

	public static void removeConfusion(LivingEntity mob) {
		CONFUSED_MOBS.put(mob, 0);
	}

	public static void applyConfusion(Plugin plugin, int ticks, LivingEntity mob) {
		if (isBoss(mob) || !(mob instanceof Mob) || mob.getScoreboardTags().contains(CrowdControlImmunityBoss.identityTag)) {
			return;
		}

		if (mobsTracker == null || mobsTracker.isCancelled()) {
			startTracker(plugin);
		}

		Mob creature = (Mob) mob;

		if (mob instanceof Vex) {
			mob.setVelocity(new Vector(0, 0, 0));
		}

		/* Fake "event" so bosses can handle being confused if they need to */
		BossManager.getInstance().entityConfused(mob);

		List<LivingEntity> nearby = getNearbyMobs(mob.getLocation(), 8, mob);
		if (nearby.size() > 0) {
			creature.setTarget(nearby.get(0));
		} else {
			creature.setTarget(null);
		}
		PotionUtils.applyPotion(null, mob, new PotionEffect(PotionEffectType.SPEED, ticks, 2, false, true));

		Integer t = CONFUSED_MOBS.get(mob);
		if (t == null || t < ticks) {
			CONFUSED_MOBS.put(mob, ticks);
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
		String cmd = "summon " + type.getName() + " " + loc.getX() + " " + loc.getY() + " " + loc.getZ() + " " + nbt;
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);

		List<Entity> entities = new ArrayList<Entity>(loc.getNearbyEntities(1f, 1f, 1f));
		entities.removeIf(e -> !e.getType().equals(type));
		if (entities.size() <= 0) {
			throw new Exception("Summoned mob but no mob appeared - " + cmd);
		}

		entities.sort((left, right) -> left.getLocation().distance(loc) >= right.getLocation().distance(loc) ? 1 : -1);
		return entities.get(0);
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
		int increments = (int)(increment.length() * 15);
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

	private static final double MARGIN_OF_ERROR = 0.001;
	private static final int MAXIMUM_ITERATIONS = (int)(Math.log(MARGIN_OF_ERROR) / Math.log(0.5) * 2);

	/**
	 * Returns the raw damage needed to achieve a final damage multiplied by some constant, undershoots to the margin of error.
	 * <p>
	 * If raw damage is directly multiplied, then armor piercing effects make the damage exponentially more punishing.
	 * <p>
	 * IMPORTANT: do not use this method if the damage taken is not reduced by armor.
	 *
	 * @param armor	     the armor of the damagee
	 * @param toughness  the armor toughness of the damagee
	 * @param damage     the initial raw damage
	 * @param multiplier the desired multiplier for the final damage
	 * @return           the raw damage needed to achieve the desired multiplier for final damage
	 */
	public static double getDamageApproximation(double armor, double toughness, double damage, double multiplier) {
		double rawDamageLowerBound;
		double rawDamageUpperBound;

		if (multiplier > 1) {
			rawDamageLowerBound = damage;
			rawDamageUpperBound = rawDamageLowerBound * multiplier;
		} else if (multiplier < 1) {
			rawDamageUpperBound = damage;
			rawDamageLowerBound = 0;	// Since armor gets better at lower damage, there's no good lower bound
		} else {
			return damage;
		}

		double finalDamageBaseline = calculateDamageAfterArmor(damage, armor, toughness);

		// Infinite loop safe this in case of bugs
		for (int i = 0; i < MAXIMUM_ITERATIONS; i++) {
			// No need to worry about double overflow
			double rawDamageMiddle = (rawDamageLowerBound + rawDamageUpperBound) / 2;
			// Protection is constant, evasion is already factored in
			double damageRatio = calculateDamageAfterArmor(rawDamageMiddle, armor, toughness) / finalDamageBaseline;

			if (damageRatio <= multiplier) {
				if (damageRatio > multiplier * (1 - MARGIN_OF_ERROR)) {
					return rawDamageMiddle;
				}
				rawDamageLowerBound = rawDamageMiddle;
			} else {
				rawDamageUpperBound = rawDamageMiddle;
			}
		}

		// Can only reach because of bug, so return a very noticeable fail case
		return 0;
	}

	public static double getDamageApproximation(EntityDamageByEntityEvent event, double multiplier) {
		// Not affected by armor
		if (!PHYSICAL_DAMAGE.contains(event.getCause())) {
			return event.getDamage() * multiplier;
		}

		// Has no attributes
		if (!(event.getEntity() instanceof LivingEntity)) {
			return event.getDamage() * multiplier;
		}

		LivingEntity le = (LivingEntity) event.getEntity();

		return getDamageApproximation(le.getAttribute(Attribute.GENERIC_ARMOR).getValue(), le.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).getValue(), event.getDamage(), multiplier);
	}

	// getFinalDamage() does not work for dummy event calls, and this is fewer calculations than getRealFinalDamage()
	private static double calculateDamageAfterArmor(double damage, double armor, double toughness) {
		armor = Math.min(30, armor);
		toughness = Math.min(20, toughness);
		return damage * (1 - Math.min(20, Math.max(armor / 5, armor - damage / (2 + toughness / 4))) / 25);
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

}
