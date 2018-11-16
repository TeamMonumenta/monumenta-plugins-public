package com.playmonumenta.plugins.item.properties;

import com.playmonumenta.plugins.item.properties.ItemPropertyManager.ItemSlot;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.PlayerUtils;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.World;

public class LifeDrain implements ItemProperty {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Life Drain";
	private static final int LIFE_DRAIN_HEAL = 1;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public double onAttack(Plugin plugin, World world, Player player, LivingEntity target, double damage, int level, DamageCause cause) {
		PlayerUtils.healPlayer(player, LIFE_DRAIN_HEAL);

		return damage;
	}
}
