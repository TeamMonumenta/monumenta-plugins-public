package com.playmonumenta.plugins.depths.bosses.spells.callicarpa;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.bosses.Callicarpa;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPFlower;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class PassiveGardenTwo extends Spell {

	public static final int BASE_COOLDOWN = 400;
	public static final int SPAWN_DELAY = 1;
	public static final double SPAWN_ANIMATION_SPIN_SPEED = Math.PI / 10;
	public static final double SPAWN_ANIMATION_HELIX_RADIUS = 1.5;
	public static final String NORMAL_FLOWER_NAME = "Flower";
	public static final String[] EVOLVED_FLOWER_NAMES = {"InfernalLily", "RoyalHydrangea", "SporeBlossom", "Sunflower"};
	public static final double FLOWER_HP = 100;
	public static final double ELITE_FLOWER_HP = 200;
	public static final String ADDS_A4_LOS_POOL = "~DD2_F1_Callicarpa_Minions_A4";
	public static final String ADDS_A8_LOS_POOL = "~DD2_F1_Callicarpa_Minions_A8";
	public static final String ADDS_A15_LOS_POOL = "~DD2_F1_Callicarpa_Minions_A15";
	public static final int MAX_MOBS = 16;

	private final LivingEntity mBoss;
	private final @Nullable DepthsParty mParty;
	private final List<Location> mFlowerSpawns;
	private final List<Location> mAddsSpawns;
	private final Color mSpawnColor = Color.fromRGB(23, 110, 37);
	private final Color mSpawnFlowerColor = Color.fromRGB(230, 115, 197);
	private final Particle.DustOptions mSpawnOptions = new Particle.DustOptions(mSpawnColor, 2.5f);
	private final Particle.DustOptions mSpawnFlowerOptions = new Particle.DustOptions(mSpawnFlowerColor, 2.5f);
	private final Particle.DustOptions mEvolveFlowerOptions = new Particle.DustOptions(Color.ORANGE, 2f);
	private final String mAddsLosPool;

	private int mSpellTicks = 0;
	private int mPlayerScaledCooldown = 100;
	private boolean mSpawnedMobsLastActivation = false;

	public PassiveGardenTwo(LivingEntity boss, @Nullable DepthsParty party) {
		mBoss = boss;
		mParty = party;
		// Store the spots where flowers can be spawned.
		mFlowerSpawns = mBoss.getNearbyEntities(200, 30, 200).stream()
			.filter(e -> e.getScoreboardTags().contains(Callicarpa.FLOWER_SPOT_TAG)).map(Entity::getLocation).toList();
		// Store the spots where adds can be spawned.
		mAddsSpawns = mBoss.getNearbyEntities(200, 30, 200).stream()
			.filter(e -> e.getScoreboardTags().contains(Callicarpa.MOB_SPAWN_SPOT_TAG)).map(Entity::getLocation).toList();

		if (party != null) {
			int ascension = party.getAscension();
			if (ascension < 4) {
				mAddsLosPool = "";
			} else if (ascension < 8) {
				mAddsLosPool = ADDS_A4_LOS_POOL;
			} else if (ascension < 15) {
				mAddsLosPool = ADDS_A8_LOS_POOL;
			} else {
				mAddsLosPool = ADDS_A15_LOS_POOL;
			}
		} else {
			mAddsLosPool = "";
		}
	}

	@Override
	public void run() {
		// Handle passive cooldown
		if (mSpellTicks < mPlayerScaledCooldown) {
			mSpellTicks++;
			return;
		}

		int playerCount = PlayerUtils.playersInRange(mBoss.getLocation(), 200, true).size();
		mPlayerScaledCooldown = BASE_COOLDOWN / playerCount;
		spawnFlowers(1);

		// Spawn mobs only half the time
		if (mSpawnedMobsLastActivation) {
			mSpawnedMobsLastActivation = false;
		} else {
			if (mParty != null && mParty.getAscension() >= 4) {
				spawnMobs((int) Math.ceil((double) playerCount / 2));
			}
		}

		mSpellTicks = 0;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	public void spawnFlowers(int count) {
		spawnFlowers(count, false);
	}

	public void spawnFlowers(int count, boolean evolved) {
		// For each flower, attempt to find a random location to spawn it at, by copying the list of all possible
		// spawn locations, picking one at random, and either spawning the plant there if it isn't already occupied,
		// or removing it from the copied list, and trying again, by picking a new random location in said list.
		// If even one flower cannot spawn, that means that every possible spawning space is occupied, so break out
		// of the for loop early.
		List<Location> aboutToSpawn = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			List<Location> temp = new ArrayList<>(mFlowerSpawns);
			boolean spawned = false;
			while (!spawned && temp.size() > 0) {
				int random = FastUtils.randomIntInRange(0, temp.size() - 1);
				Location randomLoc = temp.get(random);
				if (isFlowerAtLocation(randomLoc) || aboutToSpawn.stream().anyMatch(loc -> loc.equals(randomLoc))) {
					temp.remove(random);
				} else {
					aboutToSpawn.add(randomLoc);
					doFlowerSpawnAnimation(randomLoc);
					Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
						Entity flower = LibraryOfSoulsIntegration.summon(randomLoc, NORMAL_FLOWER_NAME);
						if (flower instanceof LivingEntity livingFlower) {
							EntityUtils.setMaxHealthAndHealth(livingFlower, DepthsParty.getAscensionScaledHealth(FLOWER_HP, mParty));
							flower.setGlowing(true);
							if (evolved) {
								evolveFlower(livingFlower);
							}
						}
						randomLoc.clone().subtract(0, 1, 0).getBlock().setType(Material.OAK_WOOD);
						randomLoc.getBlock().setType(Material.POTTED_FERN);
						aboutToSpawn.remove(randomLoc);
					}, SPAWN_DELAY);
					spawned = true;
				}
			}
			// If this iteration has not spawned any flowers, it means that every spot is filled.
			// No use trying to continue spawning more flowers with the for loop.
			if (!spawned) {
				break;
			}
		}
	}

	private void evolveFlower(LivingEntity flower) {
		Location flowerLoc = flower.getLocation();
		double missingHealth = EntityUtils.getMaxHealth(flower) - flower.getHealth();
		flower.remove();
		Entity newFlower = LibraryOfSoulsIntegration.summon(flowerLoc, EVOLVED_FLOWER_NAMES[FastUtils.randomIntInRange(0, EVOLVED_FLOWER_NAMES.length - 1)]);
		if (newFlower instanceof LivingEntity livingFlower) {
			EntityUtils.setMaxHealthAndHealth(livingFlower, DepthsParty.getAscensionScaledHealth(ELITE_FLOWER_HP, mParty));
			livingFlower.setHealth(EntityUtils.getMaxHealth(livingFlower) - missingHealth);
			livingFlower.setGlowing(true);
			Vector dir = LocationUtils.getDirectionTo(mBoss.getLocation(), flowerLoc.clone().add(0, 1, 0));
			new PPFlower(Particle.REDSTONE, flowerLoc.clone().add(0, 1, 0), 5).petals(9)
				.normal(dir).data(mEvolveFlowerOptions).spawnAsBoss();
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> doFlowerSpawnAnimation(flowerLoc), SPAWN_DELAY);
		}
	}

	public void evolveRandomFlowers(int count) {
		// Get all the non-evolved flowers.
		List<Entity> evolutionCandidates = new ArrayList<>(mBoss.getLocation().getNearbyEntities(200, 30, 200).stream().filter(e -> {
			Set<String> tags = e.getScoreboardTags();
			return e.isValid() && tags.contains(Callicarpa.FLOWER_TAG) && !tags.contains(Callicarpa.FLOWER_EVOLVED_TAG);
		}).toList());

		for (int i = 0; i < count; i++) {
			// If there are no more flowers available for evolving, break out of the loop.
			if (evolutionCandidates.size() == 0) {
				break;
			}

			// Calculate the missing health in order to keep the damage done by players, on the evolved version.
			LivingEntity randomFlower = (LivingEntity) FastUtils.getRandomElement(evolutionCandidates);
			evolutionCandidates.remove(randomFlower);
			evolveFlower(randomFlower);
		}
	}

	private void doFlowerSpawnAnimation(Location loc) {
		// Spawn a burst of green particles, to hide the flower for a bit.
		new PartialParticle(Particle.REDSTONE, loc.clone().add(0, 1.5, 0), 500)
			.delta(0.3, 0.7, 0.3).extra(0).data(mSpawnOptions).spawnAsBoss();
		new PartialParticle(Particle.REDSTONE, loc.clone().add(0, 1.5, 0), 50)
			.delta(0.3, 0.7, 0.3).extra(0).data(mSpawnFlowerOptions).spawnAsBoss();
		mBoss.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 10f, 1.25f);
		mBoss.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 10f, 1.25f);

		BukkitRunnable flowerRunnable = new BukkitRunnable() {
			final Location mCurrLoc = loc.clone();
			final double mYIncrement = 0.15;
			final double mYStop = 2.6;

			double mOffsetY = 0;
			double mTheta = 0;

			@Override
			public void run() {
				// Spawn particles at two ends of the circle of the helix.
				Location helixCursor1 = mCurrLoc.clone().add(FastUtils.cos(mTheta) * SPAWN_ANIMATION_HELIX_RADIUS, 0, FastUtils.sin(mTheta) * SPAWN_ANIMATION_HELIX_RADIUS);
				Location helixCursor2 = mCurrLoc.clone().add(FastUtils.cos(mTheta + Math.PI) * SPAWN_ANIMATION_HELIX_RADIUS, 0, FastUtils.sin(mTheta + Math.PI) * SPAWN_ANIMATION_HELIX_RADIUS);

				new PartialParticle(Particle.WAX_ON, helixCursor1, 3).extra(0.75).spawnAsBoss();
				new PartialParticle(Particle.WAX_ON, helixCursor2, 3).extra(0.75).spawnAsBoss();

				// Make the helix go up by adding 0.1 to the Y value of the current location.
				mOffsetY += mYIncrement;
				mCurrLoc.add(0, mYIncrement, 0);

				if (mOffsetY >= mYStop) {
					this.cancel();
				}

				mTheta += SPAWN_ANIMATION_SPIN_SPEED;
			}
		};
		flowerRunnable.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	private boolean isFlowerAtLocation(Location loc) {
		return loc.getNearbyEntities(1, 1, 1).stream().anyMatch(e -> e.getScoreboardTags().contains(Callicarpa.FLOWER_TAG));
	}

	private void spawnMobs(int count) {
		int spawnSpotCount = mAddsSpawns.size();
		int nearbyMobsCount = EntityUtils.getNearbyMobs(mBoss.getLocation(), Callicarpa.detectionRange).size();
		// Only summon up to the mob cap. Note: nearbyMobsCount counts eventual flowers and Callicarpa.
		// Also note that flowers will still spawn past the mob cap, as they are more important.
		int finalCount = nearbyMobsCount + count > MAX_MOBS ? Math.max(0, MAX_MOBS - nearbyMobsCount) : count;

		for (int i = 0; i < finalCount; i++) {
			Location chosenLoc = mAddsSpawns.get(FastUtils.randomIntInRange(0, spawnSpotCount - 1));
			LoSPool.fromString(mAddsLosPool).spawn(chosenLoc);
			new PPFlower(Particle.REDSTONE, chosenLoc, 3).petals(5).data(mEvolveFlowerOptions).spawnAsBoss();
		}
	}
}
