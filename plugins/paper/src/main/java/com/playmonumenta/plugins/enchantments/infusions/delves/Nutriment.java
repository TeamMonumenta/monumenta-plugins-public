package com.playmonumenta.plugins.enchantments.infusions.delves;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;

public class Nutriment implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Nutriment";
	private static final double HEALING_PERCENT_PER_LEVEL = 0.015;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void onRegain(Plugin plugin, Player player, int level, EntityRegainHealthEvent event) {
		double modifiedLevel = DelveInfusionUtils.getModifiedLevel(plugin, player, level);
		event.setAmount(event.getAmount() * (1 + (HEALING_PERCENT_PER_LEVEL * modifiedLevel)));
	}
}
