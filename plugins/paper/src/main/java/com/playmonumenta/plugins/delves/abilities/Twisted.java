package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.libraryofsouls.Soul;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.point.Raycast;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Twisted {

	public static final Map<UUID, Integer> MAP_WORLD_SPAWN_COUNT = new HashMap<>();

	public static final Map<String, Integer> MAP_R3_POI_SPAWN_COUNT = new HashMap<>();

	public static final String TWISTED_MINIBOSS_TAG = "TwistedMiniBoss";

	private static final String POOL_NAME = "~Twisted";
	private static final String POOL_NAME_NORMAL = "~TwistedNormal";
	private static final String POOL_NAME_WATER = "~TwistedWater";

	private static final int ANIMATION_DURATION = 20 * 2;
	private static final int MAX_SPIRAL_ANIMATOR_COUNT = 3;
	private static final int MAX_DISTANCE_FROM_PLAYERS = 20;

	public static final String DESCRIPTION = "Something, everything is wrong...";

	public static Component[] rankDescription(int level) {
		return new Component[]{
				switch (level) {
					case 1 -> Component.text("M").decorate(TextDecoration.OBFUSCATED).append(Component.text("or")).append(Component.text("tu").decorate(TextDecoration.OBFUSCATED)).append(Component.text("i non mo")).append(Component.text("rd").decorate(TextDecoration.OBFUSCATED)).append(Component.text("ent"));
					case 2 -> Component.text("Mors").decorate(TextDecoration.OBFUSCATED).append(Component.text(" non a")).append(Component.text("ccip").decorate(TextDecoration.OBFUSCATED)).append(Component.text("it excusatio")).append(Component.text("nes").decorate(TextDecoration.OBFUSCATED));
					case 3 -> Component.text("Quid").append(Component.text("quid in").decorate(TextDecoration.OBFUSCATED)).append(Component.text(" altum ")).append(Component.text("for").decorate(TextDecoration.OBFUSCATED)).append(Component.text("tuna ")).append(Component.text("tulit").decorate(TextDecoration.OBFUSCATED)).append(Component.text(", ruitura ")).append(Component.text("levat.").decorate(TextDecoration.OBFUSCATED));
					case 4 -> Component.text("Nec").decorate(TextDecoration.OBFUSCATED).append(Component.text(" vita ")).append(Component.text("nec").decorate(TextDecoration.OBFUSCATED)).append(Component.text(" fortuna ")).append(Component.text("hominibus ").decorate(TextDecoration.OBFUSCATED)).append(Component.text(" perpes ")).append(Component.text("est").decorate(TextDecoration.OBFUSCATED));
					case 5 -> Component.text("For").append(Component.text("tu").decorate(TextDecoration.OBFUSCATED)).append(Component.text("na ")).append(Component.text("fav").decorate(TextDecoration.OBFUSCATED)).append(Component.text("et fo")).append(Component.text("rtib").decorate(TextDecoration.OBFUSCATED)).append(Component.text("u")).append(Component.text("s").decorate(TextDecoration.OBFUSCATED));
					default -> Component.text("Lorem ipsum dolor sit amet");
				}
		};
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		List<Player> players = PlayerUtils.playersInRange(mob.getLocation(), MAX_DISTANCE_FROM_PLAYERS, true, false);
		if (players.isEmpty()) {
			return;
		}

		if (DelvesUtils.isValidTwistedMob(mob)) {
			int spawnSinceLast;

			boolean ring = DelvesUtils.getDungeonName(players.get(0)).contains("ring");
			String poiName = null;
			if (ring) {
				poiName = LocationUtils.getPoiNameFromLocation(mob.getLocation());
				if (poiName == null) {
					// Somehow in an r3 delve but not in a poi
					return;
				}
			}

			if (poiName != null) {
				spawnSinceLast = MAP_R3_POI_SPAWN_COUNT.getOrDefault(poiName, 1);
			} else {
				spawnSinceLast = MAP_WORLD_SPAWN_COUNT.getOrDefault(mob.getWorld().getUID(), 1);
			}

			if (shouldSpawn(level, spawnSinceLast) && !checkIfUnreachable(mob.getLocation())) {
				//spawn a twisted mob
				spawnTwisted(mob, spawnSinceLast < 1000);
				//a twisted mob is spawned -> resetting the counter

				spawnSinceLast = spawnSinceLast > 1000 ? spawnSinceLast - 1000 : 1;
				if (poiName != null) {
					MAP_R3_POI_SPAWN_COUNT.put(poiName, spawnSinceLast);
				} else {
					MAP_WORLD_SPAWN_COUNT.put(mob.getWorld().getUID(), spawnSinceLast);
				}
			} else {
				//twisted not spawned, increase the chance for the next event
				if (poiName != null) {
					MAP_R3_POI_SPAWN_COUNT.put(poiName, 1 + spawnSinceLast);
				} else {
					MAP_WORLD_SPAWN_COUNT.put(mob.getWorld().getUID(), 1 + spawnSinceLast);
				}
			}
		}
	}

	public static boolean shouldSpawn(int level, int spawns) {
		BigDecimal randomChance = BigDecimal.valueOf(FastUtils.RANDOM.nextDouble());
		BigDecimal chance = getSpawnChance(level, spawns).multiply(BigDecimal.valueOf(2));
		return !(randomChance.subtract(chance).doubleValue() > 0);
	}

	public static boolean checkIfUnreachable(Location loc) {
		// if there is bedrock between the spawner and nearby players, don't spawn a Twisted

		for (Player p : PlayerUtils.playersInRange(loc, MAX_DISTANCE_FROM_PLAYERS, true, false)) {
			Location l = loc.clone();
			Raycast raycast = new Raycast(l, p.getLocation());
			raycast.mThroughBlocks = true;
			List<Block> blocks = raycast.shootRaycast().getBlocks();

			// if there aren't any unbreakable blocks in the way, then it is not unreachable (there is some path)
			if (blocks.stream().allMatch(BlockUtils::canBeBroken)) {
				return false;
			}
		}

		// otherwise, we iterated through all players and found no reachable path
		return true;
	}


	// formula reference https://media.discordapp.net/attachments/981850439781847060/990909284483215370/unknown.png
	public static BigDecimal getSpawnChance(int level, int spawnsSinceLastTwisted) {
		if (spawnsSinceLastTwisted <= 50 - 10 * level) {
			//lower limit
			return BigDecimal.ZERO;
		}
		if (spawnsSinceLastTwisted >= 250 - 10 * level) {
			//upper limit limit
			return BigDecimal.ONE;
		}
		BigDecimal exp = BigDecimal.ONE.divide(BigDecimal.valueOf(0.005).multiply(BigDecimal.valueOf(level)), RoundingMode.HALF_UP);
		BigDecimal numerator = exp.pow(spawnsSinceLastTwisted).multiply(BigDecimal.valueOf(Math.pow(Math.E, -exp.doubleValue())));
		BigDecimal fact = new BigDecimal(FastUtils.bigFact(spawnsSinceLastTwisted));
		return numerator.divide(fact, RoundingMode.HALF_UP);
	}

	public static void spawnTwisted(LivingEntity mob, boolean normalSummon) {
		List<LivingEntity> mobsInArea = EntityUtils.getNearbyMobs(mob.getLocation(), 16);
		mobsInArea.remove(mob);
		Location spawningLoc = mob.getLocation().clone();

		spawningLoc.getWorld().playSound(spawningLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 0.1f, 0.2f);
		spawningLoc.getWorld().playSound(spawningLoc, Sound.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 0.3f, 0.1f);
		spawningLoc.getWorld().playSound(spawningLoc, Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.HOSTILE, 1.2f, 0.1f);
		spawningLoc.getWorld().playSound(spawningLoc, Sound.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.HOSTILE, 1.4f, 0.3f);
		spawningLoc.getWorld().playSound(spawningLoc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.HOSTILE, 1.6f, 0.1f);
		spawningLoc.getWorld().playSound(spawningLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 5f, 0.1f);
		int count = MAX_SPIRAL_ANIMATOR_COUNT;
		for (LivingEntity le : mobsInArea) {
			final int countFinal = count;
			new BukkitRunnable() {
				int mTimer = 0;
				final boolean mShouldSpawnSpiral = countFinal > 0;
				final Location mLocation = le.getEyeLocation();
				final double mDistance = mLocation.distance(spawningLoc);
				final Vector mDirection = spawningLoc.clone().subtract(mLocation).toVector().normalize().multiply(mDistance / (ANIMATION_DURATION / 2.0));
				final PartialParticle mPPRay = new PartialParticle(Particle.REDSTONE, mLocation, 1, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(127, 0, 0), 1.0f));
				final PartialParticle mPPRay2 = new PartialParticle(Particle.SOUL_FIRE_FLAME, mLocation, 1, 0, 0, 0);
				final PartialParticle mPPSpiral = new PartialParticle(Particle.REDSTONE, mLocation, 1, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(127, 0, 0), 1.0f));
				final PartialParticle mPPSpiral2 = new PartialParticle(Particle.SOUL_FIRE_FLAME, mLocation, 1, 0, 0, 0);
				final PartialParticle mPPSpiral3 = new PartialParticle(Particle.SMOKE_LARGE, mLocation, 1, 0, 0, 0);

				final double mZDiff = mLocation.getZ() - spawningLoc.getZ();
				final double mXDiff = mLocation.getX() - spawningLoc.getX();
				final double mHeightStep = 0.03;
				double mRadiant = 2 * Math.atan(mZDiff / (mXDiff + Math.sqrt(mXDiff * mXDiff + mZDiff * mZDiff)));
				double mCurrentHeight = mHeightStep;
				@Override
				public void run() {
					if (mTimer <= ANIMATION_DURATION / 2.0) {
						mPPRay.location(mLocation).spawnAsBoss();
						mPPRay2.location(mLocation).spawnAsBoss();
						mLocation.add(mDirection);
					} else if (mShouldSpawnSpiral) {
						for (double degree = mRadiant; degree <= mRadiant + 15; degree += 5) {
							Location l = spawningLoc.clone().add(FastUtils.cos(degree) * 1.2, mCurrentHeight, FastUtils.sin(degree) * 1.2);
							mCurrentHeight += mHeightStep;
							mPPSpiral.location(l).spawnAsBoss();
							mPPSpiral2.location(l).spawnAsBoss();
							mPPSpiral3.location(l).spawnAsBoss();
						}
						mRadiant += 15;
					}

					if (mTimer >= ANIMATION_DURATION) {
						cancel();
						return;
					}

					if (!mShouldSpawnSpiral && mTimer >= ANIMATION_DURATION / 2) {
						cancel();
						return;
					}
					mTimer++;
				}
			}.runTaskTimer(Plugin.getInstance(), 0, 1);
			count--;
		}

		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			LivingEntity twistedMob = null;
			boolean isWaterLoc = BlockUtils.containsWater(spawningLoc.getBlock());
			String pool = isWaterLoc ? POOL_NAME_WATER : normalSummon ? POOL_NAME : POOL_NAME_NORMAL;
			Map<Soul, Integer> mobsPool = LibraryOfSoulsIntegration.getPool(pool);
			if (mobsPool != null) {
				for (Map.Entry<Soul, Integer> entry : mobsPool.entrySet()) {
					twistedMob = (LivingEntity) entry.getKey().summon(spawningLoc);
				}
			}
			if (twistedMob == null) {
				MMLog.warning("[Delve - Twisted] summoned twisted with null object! THIS IS A BUG!");
				return;
			}
			twistedMob.setInvulnerable(true);
			twistedMob.setGravity(false);
			final LivingEntity twistedMobFinal = twistedMob;
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
				twistedMobFinal.setGravity(true);
				twistedMobFinal.setInvulnerable(false);
				twistedMobFinal.getWorld().playSound(twistedMobFinal.getLocation(), Sound.ENTITY_ENDER_DRAGON_AMBIENT, SoundCategory.HOSTILE, 10, 0.8f);
			}, ANIMATION_DURATION / 2);
		}, ANIMATION_DURATION / 2);
	}

	public static void despawnTwistedMiniBoss(LivingEntity mob) {
		int spawnSinceLast = MAP_WORLD_SPAWN_COUNT.getOrDefault(mob.getWorld().getUID(), 1);
		MAP_WORLD_SPAWN_COUNT.put(mob.getWorld().getUID(), spawnSinceLast + 1000);
	}

	public static void despawnR3TwistedMiniBoss(String poiName) {
		int spawnSinceLast = MAP_R3_POI_SPAWN_COUNT.getOrDefault(poiName, 1);
		MAP_R3_POI_SPAWN_COUNT.put(poiName, spawnSinceLast + 1000);
	}


}
