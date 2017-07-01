package pe.project.tracking;

import java.util.List;

import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;

import pe.project.Main;

public class TrackingManager {
	public TrackingManager(Main main, World world) {
		List<Entity> entities = world.getEntities();
		for (Entity entity : entities) {
			addEntity(entity);
		}
	}
	
	public void addEntity(Entity entity) {
		if (entity instanceof Player) {
			mPlayers.addEntity(entity);
		} else if (entity instanceof Creeper) {
			mCreepers.addEntity(entity);
		} else if (entity instanceof Boat) {
			mBoats.addEntity(entity);
		} else if (entity instanceof Silverfish) {
			mSilverfish.addEntity(entity);
		}
	}
	
	public void removeEntity(Entity entity) {
		if (entity instanceof Player) {
			mPlayers.removeEntity(entity);
		} else if (entity instanceof Creeper) {
			mCreepers.removeEntity(entity);
		} else if (entity instanceof Boat) {
			mBoats.removeEntity(entity);
		} else if (entity instanceof Silverfish) {
			mSilverfish.removeEntity(entity);
		}
	}
	
	public void update(World world) {
		mPlayers.update(world);
		mCreepers.update(world);
		mBoats.update(world);
		mSilverfish.update(world);
	}
	
	public PlayerTracking mPlayers = new PlayerTracking();
	public CreeperTracking mCreepers = new CreeperTracking();
	public BoatTracking mBoats = new BoatTracking();
	public SilverfishTracking mSilverfish = new SilverfishTracking();
}
