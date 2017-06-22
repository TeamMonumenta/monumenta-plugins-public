package pe.project.tracking;

import org.bukkit.World;
import org.bukkit.entity.Entity;

public interface EntityTracking {
	public void addEntity(Entity entity);
	
	public void removeEntity(Entity entity);
	
	public void update(World world);
}
