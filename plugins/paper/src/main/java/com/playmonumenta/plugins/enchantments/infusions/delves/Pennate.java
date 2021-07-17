package com.playmonumenta.plugins.enchantments.infusions.delves;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;

public class Pennate implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Pennate";
	private static final double REDUCT_PCT_PER_LEVEL = 0.05;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void onHurt(Plugin plugin, Player player, int level, EntityDamageEvent event) {
		double modifiedLevel = DelveInfusionUtils.getModifiedLevel(plugin, player, level);
		if (event.getCause() == DamageCause.FALL) {
			event.setDamage(event.getDamage() * (1.0 - (REDUCT_PCT_PER_LEVEL * modifiedLevel)));
		}
	}
}
