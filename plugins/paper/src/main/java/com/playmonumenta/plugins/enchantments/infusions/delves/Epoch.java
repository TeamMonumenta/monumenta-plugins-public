package com.playmonumenta.plugins.enchantments.infusions.delves;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;

public class Epoch implements BaseEnchantment {
	public static final String PROPERTY_NAME = ChatColor.GRAY + "Epoch";
	public static final double COOLDOWN_REDUCTION_PER_LEVEL = 0.0125;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	public static double getCooldownPercentage(Plugin plugin, Player player) {
		return - COOLDOWN_REDUCTION_PER_LEVEL * DelveInfusionUtils.getModifiedLevel(plugin, player, plugin.mTrackingManager.mPlayers.getPlayerCustomEnchantLevel(player, Epoch.class));
	}

}
