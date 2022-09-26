package com.playmonumenta.plugins.delves.abilities;


import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.BlockLockBoss;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class Riftborn {

	public static final String DESCRIPTION = "Spawners have a chance of generating portals.";

	public static final String[][] RANK_DESCRIPTIONS = {
		{
			"There is a 7.5% chance to spawn",
			"a void gate near a spawner, which",
			"summons rift enemies and protects",
			"the spawner from destruction."
		}
	};

	public static void spawnGate(Block block) {
		List<Location> validSpawnLocs = new ArrayList<>();
		for (int i = -1; i < 2; i++) {
			for (int j = -1; j < 2; j++) {
				Location loc = block.getLocation().clone().add(i, 0, j);
				if (!(i == 0 && j == 0) && !loc.getBlock().isSolid()) {
					validSpawnLocs.add(loc.clone().add(0.5, 1, 0.5));
				}
			}
		}
		if (!block.getLocation().clone().add(0, 1, 0).getBlock().isSolid()) {
			validSpawnLocs.add(block.getLocation().clone().add(0.5, 1, 0.5));
		}
		if (validSpawnLocs.size() > 0) {
			Location loc = validSpawnLocs.get(FastUtils.RANDOM.nextInt(validSpawnLocs.size()));
			LivingEntity boss = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "VoidGate");
			boss.addScoreboardTag(BlockLockBoss.identityTag + String.format("[x=%s,y=%s,z=%s]", block.getX(), block.getY(), block.getZ()));
			new BukkitRunnable() {
				int mTicks = 0;
				@Override
				public void run() {
					if (mTicks < 5) {
						boss.getWorld().playSound(loc, Sound.ITEM_HONEY_BOTTLE_DRINK, 10, 3f);
					} else {
						this.cancel();
					}
					mTicks++;
				}
			}.runTaskTimer(Plugin.getInstance(), 0, 5);
			try {
				BossManager.createBoss(null, boss, BlockLockBoss.identityTag);
			} catch (Exception e) {
				Plugin.getInstance().getLogger().warning("Failed to create boss BlockLockBoss: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public static void applyModifiers(Block block, int level) {
		if (level == 0) {
			return;
		}
		if (!block.hasMetadata("RiftChecked")) {
			block.setMetadata("RiftChecked", new FixedMetadataValue(Plugin.getInstance(), true));
			if (FastUtils.RANDOM.nextDouble() < 0.075) {
				spawnGate(block);
			}
		}
	}
}
