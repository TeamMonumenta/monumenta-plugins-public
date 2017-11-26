package pe.project.tracking;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Silverfish;

import pe.project.Plugin;
import pe.project.managers.LocationUtils;
import pe.project.managers.LocationUtils.LocationType;

public class SilverfishTracking implements EntityTracking {
	Plugin mPlugin = null;
	private Set<Silverfish> mEntities = new HashSet<Silverfish>();

	SilverfishTracking(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public void addEntity(Entity entity) {
		mEntities.add((Silverfish)entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		mEntities.remove(entity);
	}

	@Override
	public void update(World world, int ticks) {
		Iterator<Silverfish> silverfishIter = mEntities.iterator();
		while (silverfishIter.hasNext()) {
			Silverfish silverfish = silverfishIter.next();
			if (silverfish != null && silverfish.isValid()) {
				if (LocationUtils.getLocationType(mPlugin, silverfish) != LocationType.None) {
					silverfish.remove();
					silverfishIter.remove();
				}
			} else {
				silverfishIter.remove();
			}
		}
	}

	@Override
	public void unloadTrackedEntities() {
		mEntities.clear();
	}
}
