package com.playmonumenta.plugins.spawners.types;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTTileEntity;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import static com.playmonumenta.plugins.utils.SpawnerUtils.PROTECTOR_ATTRIBUTE;
import static com.playmonumenta.plugins.utils.SpawnerUtils.isSpawner;

public class ProtectorSpawner {
	public static boolean getProtector(ItemStack spawnerItem) {
		if (!isSpawner(spawnerItem)) {
			return false;
		}

		NBTItem item = new NBTItem(spawnerItem);

		if (!item.hasTag(PROTECTOR_ATTRIBUTE)) {
			return false;
		}

		return item.getBoolean(PROTECTOR_ATTRIBUTE);
	}

	public static boolean getProtector(Block spawnerBlock) {
		if (!isSpawner(spawnerBlock)) {
			return false;
		}

		NBTCompound dataContainer = new NBTTileEntity(spawnerBlock.getState()).getPersistentDataContainer();

		if (!dataContainer.hasTag(PROTECTOR_ATTRIBUTE)) {
			return false;
		}

		return dataContainer.getBoolean(PROTECTOR_ATTRIBUTE);
	}

	public static void setProtector(ItemStack spawnerItem, boolean protector) {
		if (!isSpawner(spawnerItem)) {
			return;
		}

		NBTItem item = new NBTItem(spawnerItem);
		item.setBoolean(PROTECTOR_ATTRIBUTE, protector);
		spawnerItem.setItemMeta(item.getItem().getItemMeta());
	}

	public static void setProtector(Block spawnerBlock, boolean protector) {
		if (!isSpawner(spawnerBlock)) {
			return;
		}

		NBTTileEntity tileEntity = new NBTTileEntity(spawnerBlock.getState());
		tileEntity.getPersistentDataContainer().setBoolean(PROTECTOR_ATTRIBUTE, protector);
	}
}
