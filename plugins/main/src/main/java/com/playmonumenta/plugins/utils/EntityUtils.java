package com.playmonumenta.plugins.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Dolphin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.PolarBear;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Rabbit.Type;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.SplashPotion;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import com.playmonumenta.nms.utils.NmsEntityUtils;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.Inferno;
import com.playmonumenta.plugins.events.CustomDamageEvent;

public class EntityUtils {

	public static final String MOB_IS_STUNNED_METAKEY = "MobIsStunnedByEntityUtils";
	public static final String MOB_IS_CONFUSED_METAKEY = "MobIsConfusedByEntityUtils";

	public static boolean isUndead(LivingEntity mob) {
		EntityType type = mob.getType();
		return type == EntityType.ZOMBIE || type == EntityType.ZOMBIE_VILLAGER || type == EntityType.PIG_ZOMBIE || type == EntityType.HUSK ||
		       type == EntityType.SKELETON || type == EntityType.WITHER_SKELETON || type == EntityType.STRAY ||
		       type == EntityType.WITHER || type == EntityType.ZOMBIE_HORSE || type == EntityType.SKELETON_HORSE ||
		       type == EntityType.PHANTOM || type == EntityType.DROWNED;
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
		if ((entity instanceof Monster || entity instanceof Slime || entity instanceof Ghast || entity instanceof PolarBear || entity instanceof Phantom || entity instanceof Shulker)
		    && !entity.getScoreboardTags().contains("SkillImmune")) {
			return true;
		} else if (entity instanceof Wolf) {
			return ((Wolf)entity).isAngry() || entity.getScoreboardTags().contains("boss_targetplayer");
		} else if (entity instanceof Rabbit) {
			return ((Rabbit)entity).getRabbitType() == Type.THE_KILLER_BUNNY;
		} else if (entity instanceof PolarBear || entity instanceof IronGolem || entity instanceof Dolphin || entity instanceof Snowman) {
			LivingEntity target = ((Mob)entity).getTarget();
			return entity.getScoreboardTags().contains("boss_targetplayer") || (target != null && target instanceof Player);
		} else if (entity instanceof Player) {
			return AbilityManager.getManager().isPvPEnabled((Player)entity);
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

	public static LivingEntity GetEntityAtCursor(Player player, int range, boolean targetPlayers, boolean targetNonPlayers, boolean checkLos) {
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

		int bx, by, bz;

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

	public static Projectile spawnArrow(Plugin plugin, Player player, Vector rotation, Vector offset, Vector speed, Class <? extends Arrow > arrowClass) {
		Location loc = player.getEyeLocation();
		loc.add(offset);
		loc.setPitch(loc.getPitch() + (float)rotation.getX());
		loc.setYaw(loc.getYaw() + (float)rotation.getY());
		Vector vel = new Vector(loc.getDirection().getX() * speed.getX(), loc.getDirection().getY() * speed.getY(), loc.getDirection().getZ() * speed.getZ());

		World world = player.getWorld();
		Arrow arrow = world.spawnArrow(loc, vel, 0.6f, 12.0f, arrowClass);

		arrow.setShooter(player);
		arrow.setVelocity(vel);

		return arrow;
	}

	public static List<Projectile> spawnArrowVolley(Plugin plugin, Player player, int numProjectiles, double speedModifier, double spacing, Class <? extends Arrow > arrowClass) {
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

	public static SplashPotion spawnCustomSplashPotion(World world, Player player, ItemStack potionStack, Location loc) {
		SplashPotion potion = (SplashPotion)world.spawnEntity(loc.add(0, 0.5, 0), EntityType.SPLASH_POTION);
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
	public static List<LivingEntity> getNearbyMobs(Location loc, double rx, double ry, double rz, Set<EntityType> types) {
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

	public static List<LivingEntity> getNearbyMobs(Location loc, double radius, Set<EntityType> types) {
		return getNearbyMobs(loc, radius, radius, radius, types);
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
		if (!target.isDead() && !target.isInvulnerable()) {
			if (callEvent) {
				CustomDamageEvent event = new CustomDamageEvent(damager, target, damage, magicType);
				event.setSpell(spell);
				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					return;
				}
				damage = event.getDamage();
			}
			if (target.getNoDamageTicks() == target.getMaximumNoDamageTicks()) {
				target.setNoDamageTicks(0);
			}

			// We want Precision Strike to trigger Swift Cuts
			if (spell == Spells.PRECISION_STRIKE || !(damager instanceof Player)) {
				// Applies DamageCause.ENTITY_ATTACK
				target.damage(damage, damager);
			} else if (damager instanceof Player) {
				// Applies DamageCause.CUSTOM
				NmsEntityUtils.customDamageEntity(target, damage, (Player) damager, "magic");
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

	public static void applyFire(Plugin plugin, int ticks, LivingEntity mob) {
		mob.setMetadata(Inferno.SET_FIRE_TICK_METAKEY, new FixedMetadataValue(plugin, mob.getTicksLived()));
		mob.setMetadata(Inferno.FIRE_TICK_METAKEY, new FixedMetadataValue(plugin, mob.getTicksLived()));
		mob.setFireTicks(ticks);
	}

	private static final Particle.DustOptions STUN_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 100), 1.0f);

	public static void applyStun(Plugin plugin, int ticks, LivingEntity mob) {
		if (isBoss(mob)) {
			return;
		}

		if (mob instanceof Mob) {
			((Mob) mob).setTarget(null);
		}

		if (mob instanceof Vex) {
			mob.setVelocity(new Vector(0, 0, 0));
		}

		mob.setMetadata(MOB_IS_STUNNED_METAKEY, new FixedMetadataValue(plugin, null));

		new BukkitRunnable() {
			int t = 0;
			double rotation = 0;
			double originalMovementSpeed = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();

			@Override
			public void run() {
				if (mob.isDead()) {
					this.cancel();
				}
				mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0);

				t++;
				rotation += 20;

				double radian1 = Math.toRadians(rotation);
				Location l = mob.getLocation();
				l.add(Math.cos(radian1) * 0.5, mob.getHeight(), Math.sin(radian1) * 0.5);
				for (int i = 0; i < 5; i++) {
					mob.getWorld().spawnParticle(Particle.REDSTONE, l, 1, 0, 0, 0, STUN_COLOR);
				}
				l.subtract(Math.cos(radian1) * 0.5, mob.getHeight(), Math.sin(radian1) * 0.5);

				if (t >= ticks) {
					if (mob.hasMetadata(MOB_IS_STUNNED_METAKEY)) {
						mob.removeMetadata(MOB_IS_STUNNED_METAKEY, plugin);
					}
					mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(originalMovementSpeed);
					this.cancel();
				}
			}

		}.runTaskTimer(plugin, 0, 1);
	}

	private static final Particle.DustOptions CONFUSION_COLOR = new Particle.DustOptions(Color.fromRGB(62, 0, 102), 1.0f);

	public static void applyConfusion(Plugin plugin, int ticks, LivingEntity mob) {
		if (isBoss(mob) || !(mob instanceof Mob)) {
			return;
		}

		Mob creature = (Mob)mob;

		if (mob instanceof Vex) {
			mob.setVelocity(new Vector(0, 0, 0));
		}

		creature.setTarget(null);
		PotionUtils.applyPotion(null, mob, new PotionEffect(PotionEffectType.SPEED, ticks, 2, false, true));
		mob.setMetadata(MOB_IS_CONFUSED_METAKEY, new FixedMetadataValue(plugin, null));

		for (LivingEntity targetMob : getNearbyMobs(mob.getLocation(), 8, mob)) {
			mob.setMetadata(EntityUtils.MOB_IS_CONFUSED_METAKEY, new FixedMetadataValue(plugin, null));
			creature.setTarget(targetMob);
			new BukkitRunnable() {
				int t = 0;
				double rotation = 0;

				@Override
				public void run() {
					if (mob.isDead()) {
						this.cancel();
					}
					t++;
					rotation += 20;

					double radian1 = Math.toRadians(rotation);
					Location l = mob.getLocation();
					l.add(Math.cos(radian1) * 0.5, mob.getHeight() + 0.25, Math.sin(radian1) * 0.5);
					mob.getWorld().spawnParticle(Particle.REDSTONE, l, 2, 0, 0, 0, CONFUSION_COLOR);
					l.subtract(Math.cos(radian1) * 0.5, mob.getHeight() + 0.25, Math.sin(radian1) * 0.5);

					if (creature.getTarget() == null) {
						for (LivingEntity newTargetMob : getNearbyMobs(mob.getLocation(), 8, mob)) {
							creature.setTarget(newTargetMob);
							break;
						}
					}

					if (t >= ticks) {
						this.cancel();
						creature.setTarget(null);
						if (mob.hasMetadata(EntityUtils.MOB_IS_CONFUSED_METAKEY)) {
							mob.removeMetadata(EntityUtils.MOB_IS_CONFUSED_METAKEY, plugin);
						}
					}
				}
			}.runTaskTimer(plugin, 0, 1);
			break;
		}
	}

}
