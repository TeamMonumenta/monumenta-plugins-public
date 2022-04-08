package com.playmonumenta.plugins.abilities.delves;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.FastUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class Colossal extends DelveModifier {

	private static final double[] SPAWN_CHANCE = {
			0.05,
			0.1,
			0.15,
			0.2,
			0.25,
			0.3,
			0.35
	};

	private static final String[] COLOSSI = {
		"ColossusofTerror",
		"ColossusofChaos",
		"ColossusofEntropy"
	};

	private static final String COLOSSO_WATER = "LeviathanofChaos";

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
			}, {
				"Broken Spawners have a " + Math.round(SPAWN_CHANCE[3] * 100) + "% chance",
				"to spawn Colossi."
			}, {
				"Broken Spawners have a " + Math.round(SPAWN_CHANCE[4] * 100) + "% chance",
				"to spawn Colossi."
			}, {
				"Broken Spawners have a " + Math.round(SPAWN_CHANCE[5] * 100) + "% chance",
				"to spawn Colossi."
			}, {
				"Broken Spawners have a " + Math.round(SPAWN_CHANCE[6] * 100) + "% chance",
				"to spawn Colossi."
			}
	};

	private final double mSpawnChance;

	public Colossal(Plugin plugin, @Nullable Player player) {
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
			int airBlocks = 0;
			int waterBlocks = 0;

			for (int x = -2; x <= 2; x++) {
				for (int y = -2; y <= 2; y++) {
					for (int z = -2; z <= 2; z++) {
						Location locClone = loc.clone().add(x, y, z);
						if (locClone.getBlock().getType() == Material.WATER) {
							waterBlocks++;
						}

						if (locClone.getBlock().getType() == Material.AIR) {
							airBlocks++;
						}
					}
				}
			}

			new PartialParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0).spawnAsEnemy();
			new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 100, 0.2, 0.2, 0.2, 0.2).spawnAsEnemy();

			final int mAir = airBlocks;
			final int mWater = waterBlocks;
			new BukkitRunnable() {
				@Override
				public void run() {
					if (mAir >= mWater) {
						LibraryOfSoulsIntegration.summon(loc, COLOSSI[FastUtils.RANDOM.nextInt(COLOSSI.length)]);
					} else {
						LibraryOfSoulsIntegration.summon(loc, COLOSSO_WATER);
					}
				}
			}.runTaskLater(mPlugin, 20);
		}

		return true;
	}

}
