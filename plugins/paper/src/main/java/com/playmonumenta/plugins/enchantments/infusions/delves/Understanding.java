package com.playmonumenta.plugins.enchantments.infusions.delves;

import java.util.EnumSet;

import org.bukkit.ChatColor;

import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

public class Understanding implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Understanding";
	public static final double POINTS_PER_LEVEL = 0.25;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}
}
