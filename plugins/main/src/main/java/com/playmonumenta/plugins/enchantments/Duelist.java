package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Vindicator;
import org.bukkit.entity.Illusioner;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Witch;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

public class Duelist implements BaseEnchantment {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Duelist";

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
		if (target instanceof Vindicator || target instanceof Illusioner || target instanceof Evoker || target instanceof Vex || target instanceof Witch || target instanceof IronGolem) {
			event.setDamage(event.getDamage() + 2.5 * level);
		}
	}
}
