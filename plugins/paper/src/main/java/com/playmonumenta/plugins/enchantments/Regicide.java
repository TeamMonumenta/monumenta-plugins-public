package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;

public class Regicide implements BaseEnchantment {

	private static final String PROPERTY_NAME = ChatColor.GRAY + "Regicide";
	private static final double DAMAGE_BONUS_PER_LEVEL = 0.1;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.MAINHAND, ItemSlot.OFFHAND);
	}

	@Override
	public void onDamage(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		if (EntityUtils.isElite(target)) {
			event.setDamage(event.getDamage() * (1 + DAMAGE_BONUS_PER_LEVEL * level));
		}
	}
}
