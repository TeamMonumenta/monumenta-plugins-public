package com.playmonumenta.plugins.enchantments.curses;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseSpawnableItemEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class CurseOfEphemerality implements BaseSpawnableItemEnchantment {

	public static String PROPERTY_NAME = ChatColor.RED + "Curse of Ephemerality";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.noneOf(ItemSlot.class);
	}

	@Override
	public boolean isMultiLevel() {
		return false;
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, int level) {
		item.remove();
	}

	public static boolean isEphemeral(@Nullable ItemStack item) {
		return InventoryUtils.getCustomEnchantLevel(item, PROPERTY_NAME, false) != 0;
	}
}
