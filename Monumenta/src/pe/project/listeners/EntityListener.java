package pe.project.listeners;

import java.util.Iterator;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.TippedArrow;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import pe.project.Main;

public class EntityListener implements Listener {
	Main mPlugin;
	World mWorld;
	
	public EntityListener(Main plugin, World world) {
		mPlugin = plugin;
		mWorld = world;
	}
	
	//	Entity Spawn Event.
	@EventHandler(priority = EventPriority.LOWEST)
	public void EntitySpawnEvent(EntitySpawnEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof Creeper) {
			mPlugin.mTrackingManager.mCreepers.addEntity(entity);
		}
	}
	
	//	Entity Death Event.
	/*@EventHandler(priority = EventPriority.LOWEST)
	public void EntityDeathEvent(EntityDeathEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof Player)) {
			String customName = entity.getCustomName();
			if (customName != "") {
				if (customName.contains("Serpensia Lord")) {
					EntityUtils.createCustomSplashPotion(mWorld, entity.getLocation());
				}
				
				Bukkit.broadcastMessage(customName);
			}
		}
	}*/
	
	//	Explosion Prime Event.
	@EventHandler(priority = EventPriority.LOWEST)
	public void ExplosionPrimeEvent(ExplosionPrimeEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof TNTPrimed) {
			mPlugin.mTrackingManager.addEntity(entity);
		}
	}
	
	//	Vehicle created.
	@EventHandler(priority = EventPriority.LOWEST)
	public void VehicleCreateEvent(VehicleCreateEvent event) {
		Vehicle vehicle = event.getVehicle();
		if (vehicle instanceof Boat) {
			mPlugin.mTrackingManager.addEntity(vehicle);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void VehicleEntityCollisionEvent(VehicleEntityCollisionEvent event) {
		Entity entity = event.getEntity();
		Vehicle vehicle = event.getVehicle();
		
		if (entity.getVehicle() != vehicle) {
			if (entity instanceof Player) {
				Player player = (Player)entity;
				if (player.getGameMode() == GameMode.ADVENTURE) {
					vehicle.remove();
				}
			}
		}
	}
	
	//	An Arrow hit something.
	@EventHandler(priority = EventPriority.HIGH)
	public void ProjectileHitEvent(ProjectileHitEvent event) {
		if (event.getEntityType() == EntityType.TIPPED_ARROW) {
			Entity entity = event.getHitEntity();
			if (entity != null) {
				if (entity instanceof Player) {
					Player player = (Player)entity;
					
					if (player.isBlocking()) {
						TippedArrow arrow = (TippedArrow)event.getEntity();
						
						Vector to = player.getLocation().toVector();
						Vector from = arrow.getLocation().toVector();
						
						if (to.subtract(from).dot(player.getLocation().getDirection()) < 0) {
							PotionData data = new PotionData(PotionType.AWKWARD);
							arrow.setBasePotionData(data);
							
							if (arrow.hasCustomEffects()) {
								Iterator<PotionEffect> effectIter = arrow.getCustomEffects().iterator();
								while (effectIter.hasNext()) {
									PotionEffect effect = effectIter.next();
									arrow.removeCustomEffect(effect.getType());
								}
							}
						}
					}
				}
			}
		}
	}
}
