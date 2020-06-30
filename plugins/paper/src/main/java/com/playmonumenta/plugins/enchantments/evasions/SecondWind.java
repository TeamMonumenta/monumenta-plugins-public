package com.playmonumenta.plugins.enchantments.evasions;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

public class SecondWind implements BaseEnchantment {

	private static final String PROPERTY_NAME = ChatColor.GRAY + "Second Wind";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public boolean useEnchantLevels() {
		return false;
	}

	@Override
	public void onFatalHurt(Plugin plugin, Player player, int level, EntityDamageEvent event) {
		if (event instanceof EntityDamageByEntityEvent) {
			EvasionInfo.triggerSecondWind(plugin, player, (EntityDamageByEntityEvent) event);
		}
	}

}
