package com.playmonumenta.plugins.enchantments.infusions;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

public class Tenacity implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Tenacity";
	private static final double REDUCT_PCT_PER_LEVEL = 0.005;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void onHurtByEntity(Plugin plugin, Player player, int level, EntityDamageByEntityEvent event) {
		double reductionPct = level * REDUCT_PCT_PER_LEVEL;
		event.setDamage(event.getDamage() * (1.0 - reductionPct));
	}

}
