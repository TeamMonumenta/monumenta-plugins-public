package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.ChestLockBoss;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;

public class Astral {

	public static final String DESCRIPTION = "Sometimes, the stars gaze back.";
	private static final List<String> MOB_POOL;

	private static List<String> POSSIBLE_DESCRIPTIONS = Arrays.asList(
		"Po" + ChatColor.MAGIC + "tesn" + ChatColor.RESET + "e co" + ChatColor.MAGIC + "nsp" + ChatColor.RESET + "icere ira" + ChatColor.MAGIC + "m c" + ChatColor.RESET + "aeli?",
		"Perc" + ChatColor.MAGIC + "ipi" + ChatColor.RESET + "sne co" + ChatColor.MAGIC + "nple" + ChatColor.RESET + "xu" + ChatColor.MAGIC + "m as" + ChatColor.RESET + "trorum?",
		"Astra " + ChatColor.MAGIC + "consumu" + ChatColor.RESET + "nt omni" + ChatColor.MAGIC + "a",
		"Pe" + ChatColor.MAGIC + "r as" + ChatColor.RESET + "pera " + ChatColor.MAGIC + "ad a" + ChatColor.RESET + "stra"
	);


	public static final String[][] RANK_DESCRIPTIONS = {
		{
			POSSIBLE_DESCRIPTIONS.get(FastUtils.RANDOM.nextInt(POSSIBLE_DESCRIPTIONS.size()))
		}
	};


	static {
		MOB_POOL = Arrays.asList("PillarAlpha", "PillarBeta", "PillarGamma");
	}

	private static void summonAstral(Block block) {
		List<Location> validSpawnLocs = new ArrayList<>();
		for (int i = -1; i < 2; i++) {
			for (int j = -1; j < 2; j++) {
				@Nullable Location loc = getNearestBlockUnder(block.getLocation().clone().add(i, 0, j), 5);
				if (!(i == 0 && j == 0) && loc != null) {
					validSpawnLocs.add(loc.clone().add(0.5, 1, 0.5));
				}
			}
		}
		if (!block.getLocation().clone().add(0, 1, 0).getBlock().isSolid() && !block.getLocation().clone().add(0, 2, 0).getBlock().isSolid() && !block.getLocation().clone().add(0, 3, 0).getBlock().isSolid()) {
			validSpawnLocs.add(block.getLocation().clone().add(0.5, 1, 0.5));
		}
		if (validSpawnLocs.size() > 0) {
			Location loc = validSpawnLocs.get(FastUtils.RANDOM.nextInt(validSpawnLocs.size()));
			LivingEntity boss = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, MOB_POOL.get(FastUtils.RANDOM.nextInt(MOB_POOL.size())));
			boss.addScoreboardTag(ChestLockBoss.identityTag + String.format("[x=%s,y=%s,z=%s]", block.getX(), block.getY(), block.getZ()));
			boss.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH, 10, 3f);
			try {
				BossManager.createBoss(null, boss, ChestLockBoss.identityTag);
			} catch (Exception e) {
				Plugin.getInstance().getLogger().warning("Failed to create boss ChestLockBoss: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private static @Nullable Location getNearestBlockUnder(Location location, int distance) {
		for (int i = 0; i < distance; i++) {
			if (location.clone().add(0, -i, 0).getBlock().isSolid() && !location.clone().add(0, 1 - i, 0).getBlock().isSolid() && !location.clone().add(0, 2 - i, 0).getBlock().isSolid() && !location.clone().add(0, 3 - i, 0).getBlock().isSolid()) {
				return location.clone().add(0, -i, 0);
			}
		}
		return null;
	}

	public static void applyModifiers(Block block, int level) {
		if (level == 0 || ZoneUtils.hasZoneProperty(block.getLocation(), ZoneUtils.ZoneProperty.LOOTROOM)) {
			return;
		}
		if (!block.hasMetadata("BulletHellChecked")) {
			block.setMetadata("BulletHellChecked", new FixedMetadataValue(Plugin.getInstance(), true));
			if (FastUtils.RANDOM.nextDouble() < 0.33) {
				summonAstral(block);
			}
		}
	}
}
