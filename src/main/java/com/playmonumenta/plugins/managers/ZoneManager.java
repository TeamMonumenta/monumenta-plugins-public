package com.playmonumenta.plugins.managers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.locations.zone.SpawnEffect;
import com.playmonumenta.plugins.locations.zone.SpawnEffect.SpawnEffectType;
import com.playmonumenta.plugins.locations.zone.Zone;
import com.playmonumenta.plugins.point.Point;

public class ZoneManager {
	Plugin mPlugin;
	List<Zone> mZones = new ArrayList<Zone>();

	public ZoneManager(Plugin plugin) {
		mPlugin = plugin;
		_initPOIs();
	}

	public void applySpawnEffect(Plugin plugin, Entity entity) {
		//int shardID = plugin.mServerProperties.getShardZoneID();
		for (Zone zone : mZones) {
			if (zone.withinZone(entity.getLocation())) {
				for (SpawnEffect effect : zone.getSpawnEffects()) {
					if (true) { //(zone.getZoneShardID() == shardID) {
						if (entity.getType() == effect.getEntityType()) {
							LivingEntity creature = (LivingEntity)entity;
							String name = creature.getCustomName();
							String goalName = effect.getName();

							if ((name == null && goalName == "DEFAULT") || name.contains(goalName)) {
								SpawnEffectType type = effect.getEffectType();
								if (type == SpawnEffectType.Health) {
									double addHealth = effect.getValue();

									AttributeInstance att = creature.getAttribute(Attribute.GENERIC_MAX_HEALTH);
									double maxHealth = att.getBaseValue();

									att.setBaseValue(maxHealth + addHealth);
									creature.setHealth(maxHealth + addHealth);
								} else if (type == SpawnEffectType.Potion) {
									List<PotionEffect> potionList = effect.getPotionEffects();
									if (potionList.size() > 0) {
										for (PotionEffect currentPot : potionList) {
											creature.addPotionEffect(currentPot);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void _initPOIs() {

		// Test Case - Zombies on Sudowoodo Island get Slowness & 5 HP
		// Removed for now as I don't have the properties in yet.

		/*  List<SpawnEffect> seL0 = new ArrayList<SpawnEffect>();
		    List<PotionEffect> potL0 = new ArrayList<PotionEffect>();

		    potL0.add(new PotionEffect(PotionEffectType.GLOWING, Constants.THREE_HOURS, 0));
		    seL0.add(new SpawnEffect(EntityType.ZOMBIE, "DEFAULT", SpawnEffectType.Potion, potL0));
		    seL0.add(new SpawnEffect(EntityType.SKELETON, "A", SpawnEffectType.Potion, potL0));

		    seL0.add(new SpawnEffect(EntityType.ZOMBIE, "DEFAULT", SpawnEffectType.Health, 5.0));

		    mZones.add(new Zone("Test", 99, new Point(-50, 1,-50), new Point(50, 255, 50), seL0));
		*/

		// Swamp Nerfs
		List<SpawnEffect> seL1 = new ArrayList<SpawnEffect>();
		List<PotionEffect> slownessEffect = new ArrayList<PotionEffect>();
		slownessEffect.add(new PotionEffect(PotionEffectType.SLOW, Constants.THREE_HOURS, 0));

		seL1.add(new SpawnEffect(EntityType.CREEPER, "DEFAULT", SpawnEffectType.Health, -8.0));
		seL1.add(new SpawnEffect(EntityType.SPIDER, "DEFAULT", SpawnEffectType.Health, -4.0));
		seL1.add(new SpawnEffect(EntityType.ZOMBIE, "DEFAULT", SpawnEffectType.Health, -4.0));
		seL1.add(new SpawnEffect(EntityType.SKELETON, "DEFAULT", SpawnEffectType.Health, -8.0));
		seL1.add(new SpawnEffect(EntityType.SKELETON, "DEFAULT", SpawnEffectType.Potion, slownessEffect));

		mZones.add(new Zone("TSwamp", 1, new Point(-950, 1, -650), new Point(-90, 255, 650), seL1));
	}
}
