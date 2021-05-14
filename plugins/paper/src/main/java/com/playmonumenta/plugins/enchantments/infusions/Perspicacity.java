package com.playmonumenta.plugins.enchantments.infusions;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.events.CustomDamageEvent;

public class Perspicacity implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Perspicacity";
	private static final double DAMAGE_PCT_PER_LEVEL = 0.01;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public void onAbility(Plugin plugin, Player player, int level, LivingEntity target, CustomDamageEvent event) {
		if (event.getSpell() == null) {
			return;
		}
		double abilityDmgBuffPct = level * DAMAGE_PCT_PER_LEVEL;
		event.setDamage(event.getDamage() * (1.0 + abilityDmgBuffPct));
	}
}
