package com.playmonumenta.plugins.abilities.delves;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Legionary extends DelveModifier {

	private static final double[] SPAWN_CHANCE = {
			0.15,
			0.3,
			0.45,
			0.6,
			0.75,
			0.9,
			1.05
	};

	private final double mSpawnChance;

	public static final String DESCRIPTION = "Enemies come in larger numbers.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Spawners have a " + Math.round(SPAWN_CHANCE[0] * 100) + "% chance",
				"to spawn a copy of an enemy."
			}, {
				"Spawners have a " + Math.round(SPAWN_CHANCE[1] * 100) + "% chance",
				"to spawn a copy of an enemy."
			}, {
				"Spawners have a " + Math.round(SPAWN_CHANCE[2] * 100) + "% chance",
				"to spawn a copy of an enemy."
			}, {
				"Spawners have a " + Math.round(SPAWN_CHANCE[3] * 100) + "% chance",
				"to spawn a copy of an enemy."
			}, {
				"Spawners have a " + Math.round(SPAWN_CHANCE[4] * 100) + "% chance",
				"to spawn a copy of an enemy."
			}, {
				"Spawners have a " + Math.round(SPAWN_CHANCE[5] * 100) + "% chance",
				"to spawn a copy of an enemy."
			}, {
				"Spawners have a " + Math.round(SPAWN_CHANCE[6] * 100) + "% chance",
				"to spawn a copy of an enemy."
			}
	};

	public Legionary(Plugin plugin, @Nullable Player player) {
		super(plugin, player, Modifier.LEGIONARY);

		if (player != null) {
			int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.LEGIONARY);
			mSpawnChance = SPAWN_CHANCE[rank - 1];
		} else {
			mSpawnChance = 0;
		}
	}

	@Override
	public void applyModifiers(LivingEntity mob, SpawnerSpawnEvent event) {
		if (FastUtils.RANDOM.nextDouble() < mSpawnChance) {
			DelvesUtils.duplicateLibraryOfSoulsMob(mob);
		}
		//Chance for a third if chance > 100
		if (FastUtils.RANDOM.nextDouble() < mSpawnChance - 1) {
			DelvesUtils.duplicateLibraryOfSoulsMob(mob);
		}
	}

}
