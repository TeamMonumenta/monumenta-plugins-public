package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.listeners.EntityListener;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.EnumSet;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Spider;

public class Chivalrous {

	private static final double SPAWN_CHANCE_PER_LEVEL = 0.10;
	public static final String AVOID_CHIVALROUS = "boss_chivalrousimmune";
	public static final String CHIVALROUS_PASSENGER_TAG = "ChivalrousPassengerTag";

	// 33% bee - 33% slime - 33% cave spider
	private static final String[] MOUNTS = {
		"ChivalrousBeeMount",
		"SlimeMount",
		"ChivalrousCaveSpiderMount"
	};

	private static final String[] MOUNTS_NO_SPIDER = {
		"ChivalrousBeeMount",
		"SlimeMount"
	};

	private static final String[] LAVA_MOUNTS = {
		"ChivalrousBeeMount",
		"ChivalrousStriderMount",
		"MagmaCubeMount"
	};

	private static final String[] WATER_MOUNTS = {
		"ChivalrousBeeMount",
		"ChivalrousDolphinMount",
		"ChivalrousPufferfishMount"
	};

	public static final String[] MOUNT_NAMES = {
		"Chivalrous Bee Mount",
		"Slime Mount",
		"Magma Cube Mount",
		"Chivalrous Strider Mount",
		"Chivalrous Dolphin Mount",
		"Chivalrous Pufferfish Mount",
		"Chivalrous Cave Spider Mount"
	};

	public static final String DESCRIPTION = "Enemies become mounted on slimes, bees, and spiders.";

	public static Component[] rankDescription(int level) {
		return new Component[]{
			Component.text("Enemies have a " + Math.round(SPAWN_CHANCE_PER_LEVEL * level * 100) + "% chance to be Chivalrous, riding a mount into battle.")
		};
	}


	private static final EnumSet<EntityType> CHIVALROUS_IMMUNE = EnumSet.of(
		EntityType.GHAST,
		EntityType.PHANTOM,
		EntityType.VEX,
		EntityType.BEE,
		EntityType.BLAZE
	);

	public static void applyModifiers(LivingEntity mob, int level) {
		if (!mob.isInsideVehicle() && !CHIVALROUS_IMMUNE.contains(mob.getType()) && !EntityUtils.isBoss(mob) && !DelvesUtils.isDelveMob(mob)
			&& FastUtils.RANDOM.nextDouble() < SPAWN_CHANCE_PER_LEVEL * level && !mob.getScoreboardTags().contains(AVOID_CHIVALROUS)) {
			boolean isInWater = LocationUtils.isLocationInWater(mob.getLocation());
			boolean isInLava = mob.getLocation().getBlock().getType() == Material.LAVA;
			String[] possibleMounts = MOUNTS;
			if (isInWater) {
				possibleMounts = WATER_MOUNTS;
			} else if (isInLava) {
				possibleMounts = LAVA_MOUNTS;
			} else if (mob instanceof Spider) {
				// kind of redundant if we put a spider on top of a spider, so don't allow it
				possibleMounts = MOUNTS_NO_SPIDER;
			}
			Entity mount = LibraryOfSoulsIntegration.summon(mob.getLocation(), possibleMounts[FastUtils.RANDOM.nextInt(possibleMounts.length)]);
			if (mount != null) {
				mount.addScoreboardTag(EntityListener.BEES_BLOCK_HIVE_ENTER_EVENT);
				mount.addPassenger(mob);

				mob.addScoreboardTag(CHIVALROUS_PASSENGER_TAG);
				if (mob instanceof Creeper creeper) {
					creeper.setExplosionRadius((creeper.getExplosionRadius() + 1) / 2);
				}
			}
		}
	}

	public static boolean isChivalrousName(String name) {
		for (String s : MOUNT_NAMES) {
			if (s.equals(name)) {
				return true;
			}
		}
		return false;
	}

}
