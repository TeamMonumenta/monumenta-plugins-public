package com.playmonumenta.plugins.spawners.types;

import com.playmonumenta.plugins.particle.PPLine;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTTileEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;

import static com.playmonumenta.plugins.utils.SpawnerUtils.RALLY_ATTRIBUTE;
import static com.playmonumenta.plugins.utils.SpawnerUtils.isSpawner;

public class RallySpawner {
	public static int getRally(ItemStack spawnerItem) {
		if (!isSpawner(spawnerItem)) {
			return 0;
		}

		NBTItem item = new NBTItem(spawnerItem);

		if (!item.hasTag(RALLY_ATTRIBUTE)) {
			return 0;
		}

		return item.getInteger(RALLY_ATTRIBUTE);
	}

	public static int getRally(Block spawnerBlock) {
		if (!isSpawner(spawnerBlock)) {
			return 0;
		}

		NBTCompound dataContainer = new NBTTileEntity(spawnerBlock.getState()).getPersistentDataContainer();

		if (!dataContainer.hasTag(RALLY_ATTRIBUTE)) {
			return 0;
		}

		return dataContainer.getInteger(RALLY_ATTRIBUTE);
	}

	public static void setRally(ItemStack spawnerItem, int rally) {
		if (rally < 0 || !isSpawner(spawnerItem)) {
			return;
		}

		NBTItem item = new NBTItem(spawnerItem);
		item.setInteger(RALLY_ATTRIBUTE, rally);
		spawnerItem.setItemMeta(item.getItem().getItemMeta());
	}

	public static void setRally(Block spawnerBlock, int rally) {
		if (rally < 0 || !isSpawner(spawnerBlock)) {
			return;
		}

		NBTTileEntity tileEntity = new NBTTileEntity(spawnerBlock.getState());
		tileEntity.getPersistentDataContainer().setInteger(RALLY_ATTRIBUTE, rally);
	}

	public static void triggerRallyEffect(Block spawnerBlock, int rally) {

		// box to look for spawners
		Location center = spawnerBlock.getLocation();
		BoundingBox searchBox = new BoundingBox(
			center.getX() - rally, center.getY() - rally, center.getZ() - rally,
			center.getX() + rally, center.getY() + rally, center.getZ() + rally
		);
		for (int x = (int) searchBox.getMinX(); x <= searchBox.getMaxX(); x++) {
			for (int y = (int) searchBox.getMinY(); y <= searchBox.getMaxY(); y++) {
				for (int z = (int) searchBox.getMinZ(); z <= searchBox.getMaxZ(); z++) {
					Block block = center.getWorld().getBlockAt(x, y, z);
					if (block.getType() == Material.SPAWNER) {
						// found spawners
						CreatureSpawner spawner = (CreatureSpawner) block.getState();

						Player spawnPlayer = null;
						double closestDistanceSquared = Double.MAX_VALUE;
						for (Player player : center.getWorld().getPlayers()) {
							double distanceSquared = player.getLocation().distanceSquared(spawner.getLocation());
							if (distanceSquared < closestDistanceSquared) {
								closestDistanceSquared = distanceSquared;
								spawnPlayer = player;
							}
						}

						// fix yellow line
						if (spawnPlayer == null) {
							continue;
						}
						PPLine line = new PPLine(Particle.ENCHANTMENT_TABLE, spawnerBlock.getLocation().clone().add(0.5, 0.5, 0.5), spawner.getLocation().clone().add(0.5, 0.5, 0.5));
						line.countPerMeter(10).spawnAsEnemy();
						// make event work hopefully
						SpawnerSpawnEvent event = new SpawnerSpawnEvent(spawnPlayer, spawner);
						Bukkit.getPluginManager().callEvent(event);


					}
				}
			}
		}
	}
}
