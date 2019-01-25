package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

public class Slayer implements BaseEnchantment {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Slayer";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void onAttack(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		if (target instanceof Creeper || target instanceof Blaze || target instanceof Enderman || target instanceof Endermite) {
			event.setDamage(event.getDamage() + 2.5 * level);
		}
	}
}
