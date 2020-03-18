package com.playmonumenta.plugins.enchantments.infusions;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

public class Vigor implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Vigor";
	private static final double DAMAGE_PCT_PER_LEVEL = 0.01;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void onAttack(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		double damageBuffPct = level * DAMAGE_PCT_PER_LEVEL;
		event.setDamage(event.getDamage() * (1.0 + damageBuffPct));
	}
}