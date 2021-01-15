package com.playmonumenta.plugins.abilities.delves;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;

public class Dreadful extends DelveModifier {

	private static final double[] SPAWN_CHANCE = {
			0.2,
			0.4,
			0.6
	};

	private static final String[] DREADNAUGHTS = {
		"DreadnaughtofDoom",
		"DreadnaughtofSorrow"
	};

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
			}
	};

	private final double mSpawnChance;

	public Dreadful(Plugin plugin, Player player) {
		super(plugin, player, Modifier.DREADFUL);

		int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.DREADFUL);
		mSpawnChance = SPAWN_CHANCE[rank - 1];
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		Entity mob = event.getEntity();
		if (EntityUtils.isElite(mob) && !DelvesUtils.isDelveMob(mob)) {
			if (FastUtils.RANDOM.nextDouble() < mSpawnChance) {
				Location loc = mob.getLocation();
				World world = loc.getWorld();
				LibraryOfSoulsIntegration.summon(loc, DREADNAUGHTS[FastUtils.RANDOM.nextInt(DREADNAUGHTS.length)]);

				loc.add(0, 1, 0);
				world.spawnParticle(Particle.FLAME, loc, 50, 0, 0, 0, 0.1);
				world.spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0.5, 1, 0.5, 0);
			}
		}
	}

}
