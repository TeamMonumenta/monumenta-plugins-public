package com.playmonumenta.plugins.enchantments;

import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class CurseOfEphemerality implements BaseEnchantment {

	public static String PROPERTY_NAME = ChatColor.RED + "Curse of Ephemerality";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean useEnchantLevels() {
		return false;
	}

	@Override
	public boolean hasOnSpawn() {
		return true;
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, int level) {
		item.remove();
	}

	public static boolean isEphemeral(ItemStack item) {
		return InventoryUtils.getCustomEnchantLevel(item, PROPERTY_NAME, false) != 0;
	}
}
