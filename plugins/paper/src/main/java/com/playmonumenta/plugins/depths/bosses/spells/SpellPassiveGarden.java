package com.playmonumenta.plugins.depths.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Hedera;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpellPassiveGarden extends Spell {

	public static int PLANT_SPAWN_INTERVAL = 20 * 20;

	private Entity mLauncher;
	private int mTicks;
	public List<Location> mPlantSpawns;
	public Map<Location, LivingEntity> mPlants;
	public Map<Location, String> mPlantTypes;
	public List<String> mPlantOptions;
	public Location mResetLocation;

	public SpellPassiveGarden(LivingEntity mBoss, List<Location> plantSpawns, Map<Location, LivingEntity> plants,
			Map<Location, String> plantTypes, Location spawnPoint) {
		mLauncher = mBoss;
		mPlantSpawns = plantSpawns;
		mPlants = plants;
		mPlantTypes = plantTypes;
		mResetLocation = spawnPoint;
		mPlantOptions = new ArrayList<>();
		mPlantOptions.add("SporeDionaea");
		mPlantOptions.add("VampiricDionaea");
		mPlantOptions.add("PoisonousDionaea");
		mPlantOptions.add("FertilizerDionaea");
	}

	@Override
	public void run() {

		//Reset Hedera if she is in water
		if (mLauncher.getLocation().getBlock().isLiquid()) {
			mLauncher.teleport(mResetLocation);
		}

		//Cleanse players of poison
		for (Player player : PlayerUtils.playersInRange(mLauncher.getLocation(), Hedera.detectionRange, true)) {
			PotionEffect poisonEffect = player.getPotionEffect(PotionEffectType.POISON);
			if (poisonEffect != null) {
				player.removePotionEffect(PotionEffectType.POISON);
			}
		}

		//This function runs every 5 ticks
		mTicks += 5;
		//Update plants to see if any are dead
		updatePlants();

		//Give a warning if we're 2 seconds before plants spawn
		if ((mTicks + 40) % PLANT_SPAWN_INTERVAL == 0) {
			mLauncher.getWorld().playSound(mLauncher.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 1, 1);
			PlayerUtils.executeCommandOnNearbyPlayers(mLauncher.getLocation(), Hedera.detectionRange, "tellraw @s [\"\",{\"text\":\"[Hedera]\",\"color\":\"gold\"},{\"text\":\" Twisting vines, grow and rise! Bring forth ruin, spell demise!\",\"color\":\"dark_green\"}]");
		}

		//Also spawn if 2 seconds into the fight
		if (mTicks % PLANT_SPAWN_INTERVAL == 0 || mTicks == 40) {

			//Base case, spawn 2 plants if there are 3 or more players alive
			if (mPlants.size() <= 2) {
				//Spawn one plant
				spawnPlant();
				//Spawn another if 3 or more players
				if (PlayerUtils.playersInRange(mLauncher.getLocation(), Hedera.detectionRange, true).size() >= 3) {
					spawnPlant();
				}
			} else if (mPlants.size() == 3) {
				spawnPlant();
			}
		}
	}

	private void updatePlants() {
		for (Location l : mPlantSpawns) {
			if (mPlants.get(l) == null || mPlants.get(l).isDead()) {
				mPlants.remove(l);
				mPlantTypes.remove(l);
			}
		}
	}

	public void spawnPlant() {
		//Get an open location
		Collections.shuffle(mPlantSpawns);
		for (Location loc : mPlantSpawns) {
			List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(loc, 1.0);
			if (mPlants.get(loc) == null && mPlantTypes.get(loc) == null && nearbyMobs.size() == 0) {
				//Summon a new plant here
				String plant = getValidPlant();
				if (plant == null) {
					continue;
				}

				LivingEntity newPlant = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, plant);
				newPlant.setPersistent(true);
				mPlants.put(loc, newPlant);
				mPlantTypes.put(loc, plant);

				mLauncher.getWorld().playSound(loc, Sound.ENTITY_WITHER_BREAK_BLOCK, 20.0f, 1.0f);
				mLauncher.getWorld().playSound(loc, Sound.BLOCK_GRASS_PLACE, 20.0f, 1.0f);

				break;
			}
		}
	}


	private @Nullable String getValidPlant() {
		Collection<String> activeTypes = mPlantTypes.values();
		List<String> currentTypes = new ArrayList<>();
		for (String s : activeTypes) {
			currentTypes.add(s);
		}

		if (currentTypes.contains("SporeDionaea")) {
			currentTypes.add("ElderSporeDionaea");
		}
		if (currentTypes.contains("VampiricDionaea")) {
			currentTypes.add("ElderVampiricDionaea");
		}
		if (currentTypes.contains("PoisonousDionaea")) {
			currentTypes.add("ElderPoisonousDionaea");
		}
		if (currentTypes.contains("FertilizerDionaea")) {
			currentTypes.add("ElderFertilizerDionaea");
		}

		Collections.shuffle(mPlantOptions);

		for (String plant : mPlantOptions) {
			if (!currentTypes.contains(plant)) {
				return plant;
			}
		}

		return null;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
