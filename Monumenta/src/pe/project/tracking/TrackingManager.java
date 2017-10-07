package pe.project.tracking;

import java.util.List;

import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;

import pe.project.Constants;
import pe.project.Main;

public class TrackingManager {
	Main mPlugin = null;
	
	public PlayerTracking mPlayers;
	public CreeperTracking mCreepers;
	public BoatTracking mBoats;
	public SilverfishTracking mSilverfish;
	public FishingHookTracking mFishingHook;
	
	public TrackingManager(Main main, World world) {
		mPlugin = main;
		
		mPlayers = new PlayerTracking(mPlugin);
		mCreepers = new CreeperTracking();
		mBoats = new BoatTracking();
		mSilverfish = new SilverfishTracking();
		mFishingHook = new FishingHookTracking();
		
		List<Entity> entities = world.getEntities();
		for (Entity entity : entities) {
			addEntity(entity);
		}
	}
	
	public void unloadTrackedEntities() {
		mPlayers.unloadTrackedEntities();
		mCreepers.unloadTrackedEntities();
		mBoats.unloadTrackedEntities();
		mSilverfish.unloadTrackedEntities();
		mFishingHook.unloadTrackedEntities();
	}
	
	public void addEntity(Entity entity) {
		if (Constants.TRACKING_MANAGER_ENABLED) {
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
	
	public void update(World world, int ticks) {
		mPlayers.update(world, ticks);
		mCreepers.update(world, ticks);
		mBoats.update(world, ticks);
		mSilverfish.update(world, ticks);
	}
}
