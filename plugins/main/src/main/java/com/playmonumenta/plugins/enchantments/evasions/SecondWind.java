package com.playmonumenta.plugins.enchantments.evasions;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.events.EvasionEvent;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SecondWind implements BaseEnchantment {

	private static String PROPERTY_NAME = ChatColor.GRAY + "Second Wind";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND);
	}

	@Override
	public void onEvade(Plugin plugin, Player player, int level, EvasionEvent event) {
		PlayerUtils.healPlayer(player, level);
	}

}
