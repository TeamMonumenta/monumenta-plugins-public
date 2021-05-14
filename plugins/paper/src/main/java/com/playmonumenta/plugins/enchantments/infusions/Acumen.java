package com.playmonumenta.plugins.enchantments.infusions;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

public class Acumen implements BaseEnchantment {
	public static final String PROPERTY_NAME = ChatColor.GRAY + "Acumen";
	private static final double ACUMEN_MULTIPLIER = 0.02;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public void onExpChange(Plugin plugin, Player player, PlayerExpChangeEvent event, int level) {
		double expBuffPct = ACUMEN_MULTIPLIER * level;
		event.setAmount((int)(event.getAmount() * (1.0 + expBuffPct)));
	}
}
