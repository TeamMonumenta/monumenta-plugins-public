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

	private static final String PROPERTY_NAME = ChatColor.GRAY + "Second Wind";
	private static final double PERCENT_HEAL_CAP = 0.5;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void onEvade(Plugin plugin, Player player, int level, EvasionEvent event) {
		PlayerUtils.healPlayer(player, Math.max(event.getFinalDamage() * PERCENT_HEAL_CAP, Math.sqrt(level)));
	}

}
