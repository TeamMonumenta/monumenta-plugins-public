package com.playmonumenta.plugins.abilities.delves;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class Pernicious extends DelveModifier {

	private static final double[] SPAWN_CHANCE = {
			0.15,
			0.3,
			0.45
	};

	private final double mSpawnChance;

	public static final String DESCRIPTION = "Enemies have craftier spawning patterns.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Spawners have a " + Math.round(SPAWN_CHANCE[0] * 100) + "% chance",
				"to spawn a copy of an enemy behind a player."
			}, {
				"Spawners have a " + Math.round(SPAWN_CHANCE[1] * 100) + "% chance",
				"to spawn a copy of an enemy behind a player."
			}, {
				"Spawners have a " + Math.round(SPAWN_CHANCE[2] * 100) + "% chance",
				"to spawn a copy of an enemy behind a player."
			}
	};

	public Pernicious(Plugin plugin, Player player) {
		super(plugin, player, Modifier.PERNICIOUS);

		int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.PERNICIOUS);
		mSpawnChance = SPAWN_CHANCE[rank - 1];
	}

	@Override
	public void applyModifiers(LivingEntity mob, SpawnerSpawnEvent event) {
		if (FastUtils.RANDOM.nextDouble() < mSpawnChance) {
			for (Player player : PlayerUtils.playersInRange(mob.getEyeLocation(), 16)) {
				if (player.hasLineOfSight(mob)) {
					Location loc = getSpawnLocationBehindPlayer(mob, player);
					if (loc != null) {
						loc.getWorld().playSound(loc, Sound.ENTITY_WITCH_AMBIENT, 1.4f, 0.5f);
						DelvesUtils.duplicateLibraryOfSoulsMob(mob, loc);

						return;
					}
				}
			}
		}
	}

	private Location getSpawnLocationBehindPlayer(LivingEntity mob, Player player) {
		Vector directionBackwards = player.getLocation().getDirection().multiply(-1);
		if (directionBackwards.getX() == 0 && directionBackwards.getZ() == 0) {
			return null;
		}

		directionBackwards.setY(0).normalize();
		Vector directionSideways = new Vector(directionBackwards.getZ(), 0, -directionBackwards.getX());

		// Try random-ish locations a few times
		for (int i = 0; i < 5; i++) {
			double distanceBackwards = FastUtils.RANDOM.nextDouble() * 6 + 6;
			double distanceSideways = FastUtils.RANDOM.nextDouble() * 3 - 6;

			Location loc = player.getLocation()
					.add(directionBackwards.clone().multiply(distanceBackwards))
					.add(directionSideways.clone().multiply(distanceSideways));

			if (PlayerUtils.playersInRange(loc, 6).size() > 0) {
				continue;
			}

			BoundingBox hitbox = mob.getBoundingBox();
			hitbox.shift(loc.clone().subtract(mob.getLocation()).toVector());

			// Increase the y-level if needed
			for (int y = 0; y < 5; y++) {
				if (canSpawn(loc, hitbox)) {
					return loc;
				}

				loc.add(0, 1, 0);
			}
		}

		return null;
	}

	private boolean canSpawn(Location loc, BoundingBox hitbox) {
		World world = loc.getWorld();
		if (isObstructed(loc, hitbox)
				|| isObstructed(new Location(world, hitbox.getMinX(), hitbox.getMinY(), hitbox.getMinZ()), hitbox)
				|| isObstructed(new Location(world, hitbox.getMinX(), hitbox.getMinY(), hitbox.getMaxZ()), hitbox)
				|| isObstructed(new Location(world, hitbox.getMinX(), hitbox.getMaxY(), hitbox.getMinZ()), hitbox)
				|| isObstructed(new Location(world, hitbox.getMinX(), hitbox.getMaxY(), hitbox.getMaxZ()), hitbox)
				|| isObstructed(new Location(world, hitbox.getMaxX(), hitbox.getMinY(), hitbox.getMinZ()), hitbox)
				|| isObstructed(new Location(world, hitbox.getMaxX(), hitbox.getMinY(), hitbox.getMaxZ()), hitbox)
				|| isObstructed(new Location(world, hitbox.getMaxX(), hitbox.getMaxY(), hitbox.getMinZ()), hitbox)
				|| isObstructed(new Location(world, hitbox.getMaxX(), hitbox.getMaxY(), hitbox.getMaxZ()), hitbox)) {
			return false;
		}

		return true;
	}

	private boolean isObstructed(Location loc, BoundingBox box) {
		Block block = loc.getBlock();
		return block.getBoundingBox().overlaps(box) && !block.isLiquid();
	}

}
