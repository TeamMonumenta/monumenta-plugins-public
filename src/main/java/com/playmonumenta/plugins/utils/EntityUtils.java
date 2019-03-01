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
import org.bukkit.block.Block;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Dolphin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.PolarBear;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Rabbit.Type;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.SplashPotion;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.magic.CustomDamageEvent;
import com.playmonumenta.plugins.classes.magic.MagicType;

public class EntityUtils {
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
		if (entity instanceof Monster || entity instanceof Slime || entity instanceof Ghast || entity instanceof PolarBear || entity instanceof Phantom) {
			return true;
		} else if (entity instanceof Wolf) {
			return ((Wolf)entity).isAngry();
		} else if (entity instanceof Rabbit) {
			return ((Rabbit)entity).getRabbitType() == Type.THE_KILLER_BUNNY;
		} else if (entity instanceof PolarBear || entity instanceof IronGolem || entity instanceof Dolphin || entity instanceof Snowman) {
			LivingEntity target = ((Mob)entity).getTarget();
			return target != null && target instanceof Player;  //  If a player is the target
		}

		return false;
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

	public static void applyFreeze(Plugin plugin, int ticks, LivingEntity mob) {
		if (isBoss(mob)) {
			return;
		}

		if (!mob.hasAI()) {
			FreezeObject obj = FreezeObject.getHandle(mob);
			if (obj != null) {
				obj.setRemainingDuration(ticks);
				return;
			}
		}

		new FreezeObject(plugin, ticks, mob);
	}

	public static boolean isFrozen(LivingEntity mob) {
		return FreezeObject.getHandle(mob) != null;
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

	public static boolean hasLosToLocation(World world, Location fromLocation, Location toLocation, Vector direction, int range) {
		BlockIterator bi;
		try {
			bi = new BlockIterator(world, fromLocation.toVector(), direction, 0, range);
		} catch (IllegalStateException e) {
			return false;
		}

		int bx, by, bz;
		int dist = 0;

		int tx = toLocation.getBlockX();
		int ty = toLocation.getBlockY();
		int tz = toLocation.getBlockZ();

		while (bi.hasNext()) {
			Block b = bi.next();
			bx = b.getX();
			by = b.getY();
			bz = b.getZ();

			//  If we want to check Line of sight we want to make sure the the blocks are transparent.
			if (LocationUtils.isLosBlockingBlock(b.getType())) {
				break;
			}

			if (tx == bx && ty == by && tz == bz) {
				return true;
			} else if (dist > range) {
				return false;
			}

			dist++;
		}

		return false;
	}

	public static boolean hasPathToLocation(World world, Location fromLocation, Location toLocation, Vector direction, int range) {
		BlockIterator bi;
		try {
			bi = new BlockIterator(world, fromLocation.toVector(), direction, 0, range);
		} catch (IllegalStateException e) {
			return false;
		}

		int bx, by, bz;
		int dist = 0;

		int tx = toLocation.getBlockX();
		int ty = toLocation.getBlockY();
		int tz = toLocation.getBlockZ();

		while (bi.hasNext()) {
			Block b = bi.next();
			bx = b.getX();
			by = b.getY();
			bz = b.getZ();

			//  If we want to check Line of sight we want to make sure the the blocks are transparent.
			if (LocationUtils.isPathBlockingBlock(b.getType())) {
				break;
			}

			if (tx == bx && ty == by && tz == bz) {
				return true;
			} else if (dist > range) {
				return false;
			}

			dist++;
		}

		return false;
	}

	public static Projectile spawnArrow(Plugin plugin, Player player, Vector rotation, Vector offset, Vector speed, Class<? extends Arrow> arrowClass) {
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

	public static List<Projectile> spawnArrowVolley(Plugin plugin, Player player, int numProjectiles, double speedModifier, double spacing, Class<? extends Arrow> arrowClass) {
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

	public static List<Mob> getNearbyMobs(Location loc, double radius) {
		return getNearbyMobs(loc, radius, radius, radius);
	}

	public static List<Mob> getNearbyMobs(Location loc, double rx, double ry, double rz) {
		Collection<Entity> entities = loc.getWorld().getNearbyEntities(loc, rx, ry, rz);
		entities.removeIf(e -> !(e instanceof Mob && isHostileMob(e)));
		List<Mob> mobs = new ArrayList<Mob>(entities.size());
		for (Entity entity : entities) {
			mobs.add((Mob)entity);
		}

		return mobs;
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
		damage = damage * vulnerabilityMult(target);

		if (callEvent) {
			CustomDamageEvent event = new CustomDamageEvent(damager, target, damage, magicType);
			Bukkit.getPluginManager().callEvent(event);
			damage = event.getDamage();
		}
		if (damager != null) {
			MetadataUtils.checkOnceThisTick(plugin, damager, Constants.ENTITY_DAMAGE_NONCE_METAKEY);
			target.damage(damage, damager);
		} else {
			target.damage(damage);
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

	private static final Particle.DustOptions STUN_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 100), 1.0f);

	public static void applyStun(Plugin plugin, int ticks, LivingEntity mob) {
		if (isBoss(mob)) {
			return;
		}

		mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ticks, 8, false, true));
		mob.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, ticks, 8, false, true));
		new BukkitRunnable() {
			int t = 0;
			double rotation = 0;
			@Override
			public void run() {
				t++;
				rotation += 20;

				double radian1 = Math.toRadians(rotation);
				Location l = mob.getLocation();
				l.add(Math.cos(radian1) * 0.5, mob.getHeight(), Math.sin(radian1) * 0.5);
				for (int i = 0; i < 5; i++) {
					mob.getWorld().spawnParticle(Particle.REDSTONE, l, 1, 0, 0, 0, STUN_COLOR);
				}
				l.subtract(Math.cos(radian1) * 0.5, mob.getHeight(), Math.sin(radian1) * 0.5);

				if (mob instanceof Creature) {
					Creature c = (Creature) mob;
					if (c.getTarget() != null) {
						c.setTarget(null);
					}
				}
				if (t >= ticks) {
					this.cancel();
				}
			}

		}.runTaskTimer(plugin, 0, 1);
	}
	public static class FreezeObject {
		private static final String FREEZE_METAKEY = "MonumentaFreezeMetakey";
		private static final int TICK_PERIOD = 5;
		private int mTicksRemaining;

		public FreezeObject(Plugin plugin, int ticks, LivingEntity mob) {
			mTicksRemaining = ticks;

			mob.setMetadata(FREEZE_METAKEY, new FixedMetadataValue(plugin, this));

			new BukkitRunnable() {
				@Override
				public void run() {
					mob.setAI(false);

					plugin.mWorld.spawnParticle(Particle.SNOWBALL, mob.getLocation(), 15, 0.25, (float)(mob.getHeight() / 2), 0.25, 0);

					mTicksRemaining -= TICK_PERIOD;
					if (mTicksRemaining <= 0 || mob.isDead() || mob.hasAI()) {
						this.cancel();
						mob.setAI(true);
						mob.removeMetadata(FREEZE_METAKEY, plugin);
					}
				}
			}.runTaskTimer(plugin, 0, TICK_PERIOD);
		}

		public int getRemainingDuration() {
			return mTicksRemaining;
		}

		public void setRemainingDuration(int ticks) {
			mTicksRemaining = ticks;
		}

		public void addDuration(int ticks) {
			mTicksRemaining += ticks;
		}

		public static FreezeObject getHandle(LivingEntity mob) {
			if (mob.hasMetadata(FREEZE_METAKEY)) {
				return (FreezeObject)mob.getMetadata(FREEZE_METAKEY).get(0).value();
			}
			return null;
		}
	}
}
