package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

public class Weightless implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Weightless";

	@Override
	public @NotNull String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean isMultiLevel() {
		return false;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND);
	}

}
