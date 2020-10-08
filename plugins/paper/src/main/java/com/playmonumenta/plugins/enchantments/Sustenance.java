package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

//Sustenance - Increases all healing by 10% for each level
public class Sustenance implements BaseEnchantment {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Sustenance";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public void onRegain(Plugin plugin, Player player, int level, EntityRegainHealthEvent event) {
		double boostedHealth = event.getAmount() * (1 + (0.1 * level));
		event.setAmount(boostedHealth);
	}
}
