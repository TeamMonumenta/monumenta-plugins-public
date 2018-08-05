package com.playmonumenta.plugins.item.properties;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.item.properties.ItemPropertyManager.ItemSlot;

public class Slayer implements ItemProperty {
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
	public double onAttack(Plugin plugin, World world, Player player, LivingEntity target, double damage, int level, DamageCause cause) {
		if (target instanceof Creeper || target instanceof Blaze || target instanceof Enderman || target instanceof Endermite) {
			damage = damage + 2.5 * level;
		}

		return damage;
	}
}
