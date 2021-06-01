package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

import org.bukkit.ChatColor;

public class Locked implements BaseEnchantment {
	public static final String PROPERTY_NAME = ChatColor.GRAY + "Locked";

	public String getProperty() {
		return PROPERTY_NAME;
	}

	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.INVENTORY);
	}

}
