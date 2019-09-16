package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

public class Intuition implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Intuition";
	private static final double INTUITION_MULTIPLIER = 1.5;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean useEnchantLevels() {
		return false;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public void onExpChange(Plugin plugin, Player player, PlayerExpChangeEvent event, int level) {
		event.setAmount((int)(event.getAmount() * INTUITION_MULTIPLIER));
	}
}
