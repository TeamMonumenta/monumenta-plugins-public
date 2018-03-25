package pe.project.locations.zone;

import java.util.Set;

import org.bukkit.entity.EntityType;

public class SpawnEffect {
	public EntityType mEntityType;
	public int mHealthChange;

	public SpawnEffect(EntityType entityType, int healthChange) {
		mEntityType = entityType;
		mHealthChange = healthChange;
	}

	public EntityType getEntityType() {
		return mEntityType;
	}

	public int getHealthChange() {
		return mHealthChange;
	}
}
