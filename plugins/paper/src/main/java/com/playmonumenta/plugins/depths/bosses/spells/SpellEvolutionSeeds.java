package com.playmonumenta.plugins.depths.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class SpellEvolutionSeeds extends Spell {

	public static int DAMAGE = 40;

	public Plugin mPlugin;
	public Map<Location, LivingEntity> mPlants;
	public Map<Location, String> mPlantTypes;
	public List<String> mPlantOptions;
	public int mCooldownTicks;



	public SpellEvolutionSeeds(Plugin plugin, int cooldown, Map<Location, LivingEntity> plants, Map<Location, String> plantTypes) {
		mPlugin = plugin;
		mPlants = plants;
		mPlantTypes = plantTypes;

		mPlantOptions = new ArrayList<>();
		mPlantOptions.add("SporeDionaea");
		mPlantOptions.add("VampiricDionaea");
		mPlantOptions.add("PoisonousDionaea");
		mPlantOptions.add("FertilizerDionaea");
		mCooldownTicks = cooldown;
	}

	@Override
	public boolean canRun() {
		return mPlants.values().size() > 0;
	}

	@Override
	public void run() {

		for (Location loc : mPlants.keySet()) {
			String currentPlantType = mPlantTypes.get(loc);
			LivingEntity currentPlant = mPlants.get(loc);
			if (currentPlant != null && !currentPlant.isDead() && currentPlant.getHealth() > 0 && currentPlantType != null && mPlantOptions.contains(currentPlantType)) {
				String evolvedType = "Elder" + currentPlantType;

				//Get hp difference on old plant to conserve player damage dealt
				double hpDealt = currentPlant.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() - currentPlant.getHealth();

				//Kill existing plant
				currentPlant.teleport(new Location(currentPlant.getWorld(), currentPlant.getLocation().getX(), -100, currentPlant.getLocation().getZ()));

				LivingEntity newPlant = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, evolvedType);

				//Damage plant as much as old plant was damaged
				newPlant.setHealth(newPlant.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() - hpDealt);

				mPlantTypes.put(loc, evolvedType);
				mPlants.put(loc, newPlant);
				//Particle effects
				newPlant.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 20.0f, 1.0f);

				break;
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}

}
