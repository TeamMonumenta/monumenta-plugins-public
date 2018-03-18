package pe.project.tracking;

import java.util.List;

import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import pe.project.Constants;
import pe.project.Plugin;

public class TrackingManager {
	Plugin mPlugin = null;

	public PlayerTracking mPlayers;
	public BoatTracking mBoats;

	public TrackingManager(Plugin plugin, World world) {
		mPlugin = plugin;

		mPlayers = new PlayerTracking(mPlugin);
		mBoats = new BoatTracking(mPlugin);

		List<Entity> entities = world.getEntities();
		for (Entity entity : entities) {
			addEntity(entity);
		}
	}

	public void unloadTrackedEntities() {
		mPlayers.unloadTrackedEntities();
		mBoats.unloadTrackedEntities();
	}

	public void addEntity(Entity entity) {
		if (Constants.TRACKING_MANAGER_ENABLED) {
			if (entity instanceof Player) {
				mPlayers.addEntity(entity);
			} else if (entity instanceof Boat) {
				mBoats.addEntity(entity);
			}
		}
	}

	public void removeEntity(Entity entity) {
		if (entity instanceof Player) {
			mPlayers.removeEntity(entity);
		} else if (entity instanceof Boat) {
			mBoats.removeEntity(entity);
		}
	}

	public void update(World world, int ticks) {
		mPlayers.update(world, ticks);
		mBoats.update(world, ticks);
	}
}
