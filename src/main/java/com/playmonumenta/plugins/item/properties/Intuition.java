package com.playmonumenta.plugins.item.properties;

import com.playmonumenta.plugins.item.properties.ItemPropertyManager.ItemSlot;
import com.playmonumenta.plugins.Plugin;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;

public class Intuition implements ItemProperty {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Intuition";
	private static final double INTUITION_MULTIPLIER = 1.5;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
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
