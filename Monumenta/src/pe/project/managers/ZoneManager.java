package pe.project.managers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import pe.project.Plugin;
import pe.project.locations.zone.SpawnEffect;
import pe.project.locations.zone.Zone;
import pe.project.point.Point;

public class ZoneManager {
	Plugin mPlugin;
	List<Zone> mZones = new ArrayList<Zone>();

	public ZoneManager(Plugin plugin) {
		mPlugin = plugin;
		_initPOIs();
	}

	public void applySpawnEffect(Entity entity) {
		for (Zone zone : mZones) {
			for (SpawnEffect effect : zone.getSpawnEffects()) {

				if (entity.getType() == effect.getEntityType()) {
					LivingEntity creature = (LivingEntity)entity;
					double eHealth = creature.getHealth();
					creature.setHealth(eHealth + effect.getHealthChange());

					break;
				}
			}
		}
	}

	private void _initPOIs() {
		List<SpawnEffect> seL0 = new ArrayList<SpawnEffect>();
		seL0.add(new SpawnEffect(EntityType.ZOMBIE, -15));

		mZones.add(new Zone("Test", new Point(-50, 1,-50), new Point(50, 1, 50), seL0));
	}
}
