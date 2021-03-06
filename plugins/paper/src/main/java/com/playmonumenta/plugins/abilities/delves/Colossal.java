package com.playmonumenta.plugins.abilities.delves;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.FastUtils;

public class Colossal extends DelveModifier {

	private static final double[] SPAWN_CHANCE = {
			0.05,
			0.1,
			0.15
	};

	private static final String[] COLOSSI = {
		"ColossusofTerror",
		"ColossusofChaos"
	};

	public static final String DESCRIPTION = "Broken spawners unleash enemies.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Broken Spawners have a " + Math.round(SPAWN_CHANCE[0] * 100) + "% chance",
				"to spawn Colossi."
			}, {
				"Broken Spawners have a " + Math.round(SPAWN_CHANCE[1] * 100) + "% chance",
				"to spawn Colossi."
			}, {
				"Broken Spawners have a " + Math.round(SPAWN_CHANCE[2] * 100) + "% chance",
				"to spawn Colossi."
			}
	};

	private final double mSpawnChance;

	public Colossal(Plugin plugin, Player player) {
		super(plugin, player, Modifier.COLOSSAL);

		if (player != null) {
			int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.COLOSSAL);
			mSpawnChance = SPAWN_CHANCE[rank - 1];
		} else {
			mSpawnChance = 0;
		}
	}

	@Override
	public boolean blockBreakEvent(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (block.getType() == Material.SPAWNER && FastUtils.RANDOM.nextDouble() < mSpawnChance) {
			Location loc = block.getLocation();
			World world = loc.getWorld();

			world.spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
			world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 100, 0.2, 0.2, 0.2, 0.2);

			new BukkitRunnable() {
				@Override
				public void run() {
					LibraryOfSoulsIntegration.summon(loc, COLOSSI[FastUtils.RANDOM.nextInt(COLOSSI.length)]);
				}
			}.runTaskLater(mPlugin, 20);
		}

		return true;
	}

}
