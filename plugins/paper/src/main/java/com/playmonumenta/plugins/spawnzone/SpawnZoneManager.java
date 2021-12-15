package com.playmonumenta.plugins.spawnzone;

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
import com.playmonumenta.plugins.point.Point;
import com.playmonumenta.plugins.spawnzone.SpawnEffect.SpawnEffectType;

public class SpawnZoneManager {
	Plugin mPlugin;
	List<SpawnZone> mZones = new ArrayList<SpawnZone>();

	public SpawnZoneManager(Plugin plugin) {
		mPlugin = plugin;
		initPOIs();
	}

	public void applySpawnEffect(Plugin plugin, Entity entity) {
		for (SpawnZone zone : mZones) {
			if (zone.withinZone(entity.getLocation())) {
				for (SpawnEffect effect : zone.getSpawnEffects()) {
					if (entity.getType() == effect.getEntityType()) {
						LivingEntity creature = (LivingEntity)entity;
						String name = creature.getCustomName();
						String goalName = effect.getName();

						if (name == null ? "DEFAULT".equals(goalName) : name.contains(goalName)) {
							SpawnEffectType type = effect.getEffectType();
							if (type == SpawnEffectType.Health) {
								double addHealth = effect.getValue();

								AttributeInstance att = creature.getAttribute(Attribute.GENERIC_MAX_HEALTH);
								if (att != null) {
									double maxHealth = att.getBaseValue();

									att.setBaseValue(maxHealth + addHealth);
									creature.setHealth(maxHealth + addHealth);
								}
							} else if (type == SpawnEffectType.Potion) {
								List<PotionEffect> potionList = effect.getPotionEffects();
								if (potionList != null && potionList.size() > 0) {
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

	private void initPOIs() {

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

		mZones.add(new SpawnZone("TSwamp", 1, new Point(-950, 1, -650), new Point(-90, 255, 650), seL1));
	}
}
