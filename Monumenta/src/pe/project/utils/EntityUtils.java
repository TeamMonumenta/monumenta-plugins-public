package pe.project.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Rabbit.Type;
import org.bukkit.entity.SplashPotion;
import org.bukkit.entity.TippedArrow;
import org.bukkit.entity.Wolf;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import pe.project.Main;

public class EntityUtils {
	public static boolean isUndead(LivingEntity mob) {
		EntityType type = mob.getType();
		return type == EntityType.ZOMBIE || type == EntityType.ZOMBIE_VILLAGER || type == EntityType.PIG_ZOMBIE || type == EntityType.HUSK ||
				type == EntityType.SKELETON || type == EntityType.WITHER_SKELETON || type == EntityType.STRAY ||
				type == EntityType.WITHER || type == EntityType.ZOMBIE_HORSE || type == EntityType.SKELETON_HORSE;
	}
	
	public static boolean isEliteBoss(LivingEntity mob) {
		Set<String> tags = mob.getScoreboardTags();
		return tags.contains("Elite") || tags.contains("Boss");
	}
	
	public static boolean isHostileMob(Entity entity) {
		if (entity instanceof Monster) {
			return true;
		} else if (entity instanceof Wolf) {
			return ((Wolf)entity).isAngry();
		} else if (entity instanceof Rabbit) {
			return ((Rabbit)entity).getRabbitType() == Type.THE_KILLER_BUNNY;
		}
		
		return false;
	}
	
	public static LivingEntity GetEntityAtCursor(Player player, int range, boolean targetPlayers, boolean targetNonPlayers, boolean checkLos) {
		List<Entity> en = player.getNearbyEntities(range, range, range);
		ArrayList<LivingEntity> entities = new ArrayList<LivingEntity>();
		for( Entity e : en ) {
			//	Make sure to only get living entities.
			if( e instanceof LivingEntity ) {
				//	Make sure we should be targeting this entity.
				if( (targetPlayers && (e instanceof Player)) || (targetNonPlayers && (!(e instanceof Player))) ) {
					entities.add((LivingEntity)e);
				}
			}
		}
		
		//	If there's no living entities nearby then we should just leave as there's no reason to continue.
		if( entities.size() == 0 ) {
			return null;
		}
		
		BlockIterator bi;
		try {
			bi = new BlockIterator(player, range);
		}
		catch(IllegalStateException e) { return null; }
		
		int bx, by, bz;
		
		while( bi.hasNext() ) {
			Block b = bi.next();
			bx = b.getX();
			by = b.getY();
			bz = b.getZ();
			
			//	If we want to check Line of sight we want to make sure the the blocks are transparent.
			if (checkLos && _LosBlockingBlock(b.getType())) {
				break;
			}
			
			//	Loop through the entities and see if we hit one.
			for(LivingEntity e : entities) {
				Location loc = e.getLocation();
				double ex = loc.getX();
				double ey = loc.getY();
				double ez = loc.getZ();
				
				if( (bx - 0.75D <= ex) && (ex <= bx + 1.75D)
						&& (bz - 0.75D <= ez) && (ez <= bz + 1.75D)
						&& (by - 1.0D <= ey) && (ey <= by + 2.5D) ) {
						
					//	We got our target.
					return e;
				}
			}
		}
		
		return null;
	}
	
	public static Projectile spawnArrow(Main plugin, Player player, Vector rotation, Vector offset, Vector speed) {
		Location loc = player.getEyeLocation();
		loc.add(offset);
		loc.setPitch(loc.getPitch()+(float)rotation.getX());
		loc.setYaw(loc.getYaw()+(float)rotation.getY());
		Vector vel = new Vector(loc.getDirection().getX()*speed.getX(), loc.getDirection().getY()*speed.getY(), loc.getDirection().getZ()*speed.getZ());
		
		World world = player.getWorld();
		Arrow arrow = world.spawnArrow(loc, vel, 0.6f, 12.0f, Arrow.class);
		
		arrow.setShooter(player);
		arrow.setVelocity(vel);
		
		return arrow;
	}
	
	public static Projectile spawnTippedArrow(Main plugin, Player player, Vector rotation, Vector offset, Vector speed) {
		Location loc = player.getEyeLocation();
		loc.add(offset);
		loc.setPitch(loc.getPitch()+(float)rotation.getX());
		loc.setYaw(loc.getYaw()+(float)rotation.getY());
		Vector vel = new Vector(loc.getDirection().getX()*speed.getX(), loc.getDirection().getY()*speed.getY(), loc.getDirection().getZ()*speed.getZ());
		
		World world = player.getWorld();
		TippedArrow arrow = world.spawnArrow(loc, vel, 0.6f, 12.0f, TippedArrow.class);
		
		arrow.setShooter(player);
		arrow.setVelocity(vel);
		
		return arrow;
	}
	
	public static List<Projectile> spawnArrowVolley(Main plugin, Player player, int numProjectiles, double speedModifier, double spacing) {
		List<Projectile> projectiles = new ArrayList<Projectile>();
		
		Vector speed = new Vector(1.75 * speedModifier, 2 * speedModifier, 1.75 * speedModifier);
		
		for (double yaw = -spacing * (numProjectiles/2); yaw < spacing * ((numProjectiles / 2) + 1); yaw += spacing) {
			Projectile proj = spawnArrow(plugin, player, new Vector(0, yaw, 0), new Vector(0, 0, 0), speed);
			if (proj != null) {
				projectiles.add(proj);
			}
		}
		
		return projectiles;
	}
	
	public static List<Projectile> spawnTippedArrowVolley(Main plugin, Player player, int numProjectiles, double speedModifier, double spacing) {
		List<Projectile> projectiles = new ArrayList<Projectile>();
		
		Vector speed = new Vector(1.75 * speedModifier, 2 * speedModifier, 1.75 * speedModifier);
		
		for (double yaw = -spacing * (numProjectiles/2); yaw < spacing * ((numProjectiles / 2) + 1); yaw += spacing) {
			Projectile proj = spawnTippedArrow(plugin, player, new Vector(0, yaw, 0), new Vector(0, 0, 0), speed);
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
	
	public static SplashPotion createCustomSplashPotion(World world, Location loc) {
		SplashPotion potion = (SplashPotion)world.spawnEntity(loc.add(0, 1.5, 0), EntityType.SPLASH_POTION);
		
		return potion;
	}
	
	public static boolean withinRangeOfMonster(Player player, double range) {
		List<Entity> entities = player.getNearbyEntities(range, range, range);
		for (Entity entity : entities) {
			if (entity instanceof Monster) {
				return true;
			}
		}
		
		return false;
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
	
	private static boolean _LosBlockingBlock(Material mat) {
		if (mat.equals(Material.AIR) ||
			mat.equals(Material.GLASS) ||
			mat.equals(Material.VINE) ||
			mat.equals(Material.WEB) ||
			mat.equals(Material.WATER) ||
			mat.equals(Material.STATIONARY_WATER) ||
			mat.equals(Material.LAVA) ||
			mat.equals(Material.STATIONARY_LAVA) ||
			mat.equals(Material.CARPET) ||
			ItemUtils.isDoor(mat)) {
			return false;
		}

		return true;
	}
}
