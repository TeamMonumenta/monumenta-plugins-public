package com.playmonumenta.plugins.abilities.delves;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class Dreadful extends DelveModifier {

	private static final double[] SPAWN_CHANCE = {
			0.2,
			0.4,
			0.6,
			0.8,
			1.0
	};

	private static final String[] DREADNAUGHTS = {
		"DreadnaughtofDoom",
		"DreadnaughtofSorrow",
		"DreadnaughtofSubjugation"
	};

	private static final String DREADNAUGHT_WATER = "LeviathanofDoom";

	public static final String DESCRIPTION = "Dying elites transform into new enemies.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Dying Elites have a " + Math.round(SPAWN_CHANCE[0] * 100) + "% chance",
				"to spawn Dreadnaughts."
			}, {
				"Dying Elites have a " + Math.round(SPAWN_CHANCE[1] * 100) + "% chance",
				"to spawn Dreadnaughts."
			}, {
				"Dying Elites have a " + Math.round(SPAWN_CHANCE[2] * 100) + "% chance",
				"to spawn Dreadnaughts."
			}, {
				"Dying Elites have a " + Math.round(SPAWN_CHANCE[3] * 100) + "% chance",
				"to spawn Dreadnaughts."
			}, {
				"Dying Elites have a " + Math.round(SPAWN_CHANCE[4] * 100) + "% chance",
				"to spawn Dreadnaughts."
			}
	};

	private final double mSpawnChance;

	public Dreadful(Plugin plugin, @Nullable Player player) {
		super(plugin, player, Modifier.DREADFUL);

		if (player != null) {
			int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.DREADFUL);
			mSpawnChance = SPAWN_CHANCE[rank - 1];
		} else {
			mSpawnChance = 0;
		}
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event) {
		Entity mob = event.getEntity();
		if (EntityUtils.isElite(mob) && !DelvesUtils.isDelveMob(mob)) {
			if (FastUtils.RANDOM.nextDouble() < mSpawnChance) {
				Location loc = mob.getLocation();
				if (loc.getBlock().getType() == Material.WATER) {
					LibraryOfSoulsIntegration.summon(loc, DREADNAUGHT_WATER);
				} else {
					LibraryOfSoulsIntegration.summon(loc, DREADNAUGHTS[FastUtils.RANDOM.nextInt(DREADNAUGHTS.length)]);
				}

				loc.add(0, 1, 0);
				new PartialParticle(Particle.FLAME, loc, 50, 0, 0, 0, 0.1).spawnAsEnemy();
				new PartialParticle(Particle.SMOKE_LARGE, loc, 50, 0.5, 1, 0.5, 0).spawnAsEnemy();
			}
		}
	}

}
