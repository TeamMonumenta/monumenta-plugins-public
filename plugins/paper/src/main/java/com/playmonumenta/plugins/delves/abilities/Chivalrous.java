package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.listeners.EntityListener;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.EnumSet;
import org.bukkit.Material;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public class Chivalrous {

	private static final double[] SPAWN_CHANCE = {
			0.10,
			0.20,
			0.30,
			0.40,
			0.50
	};

	// 50% bee - 25% slime - 25% magmacube
	private static final String[] MOUNTS = {
			"ChivalrousBeeMount",
			"ChivalrousBeeMount",
			"SlimeMount",
			"MagmaCubeMount"
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
			"Chivalrous Pufferfish Mount"
	};

	public static final String DESCRIPTION = "Enemies become Knights of slime, bees, dolphins, fish, and striders.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Enemies have a " + Math.round(SPAWN_CHANCE[0] * 100) + "% chance to be Chivalrous, gaining 10% damage."
			}, {
				"Enemies have a " + Math.round(SPAWN_CHANCE[1] * 100) + "% chance to be Chivalrous, gaining 10% damage."
			}, {
				"Enemies have a " + Math.round(SPAWN_CHANCE[2] * 100) + "% chance to be Chivalrous, gaining 10% damage."
			}, {
				"Enemies have a " + Math.round(SPAWN_CHANCE[3] * 100) + "% chance to be Chivalrous, gaining 10% damage."
			}, {
				"Enemies have a " + Math.round(SPAWN_CHANCE[4] * 100) + "% chance to be Chivalrous, gaining 10% damage."
			}
	};


	private static final EnumSet<EntityType> CHIVALROUS_IMMUNE = EnumSet.of(
			EntityType.GHAST,
			EntityType.PHANTOM,
			EntityType.VEX,
			EntityType.BEE,
			EntityType.BLAZE
	);

	public static void applyModifiers(LivingEntity mob, int level) {
		if (!mob.isInsideVehicle() && !CHIVALROUS_IMMUNE.contains(mob.getType()) && !EntityUtils.isBoss(mob) && !DelvesUtils.isDelveMob(mob)
				&& FastUtils.RANDOM.nextDouble() < SPAWN_CHANCE[level - 1]) {
			boolean isInWater = LocationUtils.isLocationInWater(mob.getLocation());
			boolean isInLava = mob.getLocation().getBlock().getType() == Material.LAVA;
			String [] possibleMounts = MOUNTS;
			if (isInWater) {
				possibleMounts = WATER_MOUNTS;
			} else if (isInLava) {
				possibleMounts = LAVA_MOUNTS;
			}
			Entity mount = LibraryOfSoulsIntegration.summon(mob.getLocation(), possibleMounts[FastUtils.RANDOM.nextInt(possibleMounts.length)]);
			if (mount != null) {
				mount.addScoreboardTag(EntityListener.BEES_BLOCK_HIVE_ENTER_EVENT);
				mount.addPassenger(mob);

				if (mob instanceof Creeper creeper) {
					creeper.setExplosionRadius((creeper.getExplosionRadius() + 1) / 2);
				}
			}
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mob, "ChivalrousDamageEffect", new PercentDamageDealt(999999999, .1));
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
