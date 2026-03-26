package com.playmonumenta.plugins.spawners.types;

import de.tr7zw.nbtapi.NBT;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import static com.playmonumenta.plugins.utils.SpawnerUtils.PROTECTOR_ATTRIBUTE;
import static com.playmonumenta.plugins.utils.SpawnerUtils.isSpawner;

public class ProtectorSpawner {
	public static boolean getProtector(ItemStack spawnerItem) {
		if (!isSpawner(spawnerItem)) {
			return false;
		}

		return NBT.get(spawnerItem, nbt -> nbt.hasTag(PROTECTOR_ATTRIBUTE) && nbt.getBoolean(PROTECTOR_ATTRIBUTE));
	}

	public static boolean getProtector(Block spawnerBlock) {
		if (!isSpawner(spawnerBlock)) {
			return false;
		}

		return NBT.getPersistentData(spawnerBlock.getState(), nbt -> nbt.hasTag(PROTECTOR_ATTRIBUTE) && nbt.getBoolean(PROTECTOR_ATTRIBUTE));
	}

	public static void setProtector(ItemStack spawnerItem, boolean protector) {
		if (!isSpawner(spawnerItem)) {
			return;
		}

		NBT.modify(spawnerItem, nbt -> {
			nbt.setBoolean(PROTECTOR_ATTRIBUTE, protector);
		});
	}

	public static void setProtector(Block spawnerBlock, boolean protector) {
		if (!isSpawner(spawnerBlock)) {
			return;
		}

		NBT.modifyPersistentData(spawnerBlock.getState(), nbt -> {
			nbt.setBoolean(PROTECTOR_ATTRIBUTE, protector);
		});
	}
}
