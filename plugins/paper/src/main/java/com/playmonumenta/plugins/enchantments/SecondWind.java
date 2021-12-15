package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;



public class SecondWind implements BaseEnchantment {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Second Wind";
	private static final double DAMAGE_RESIST = 0.1;
	private static final double HEALTH_LIMIT = 0.5;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.ARMOR, ItemSlot.OFFHAND);
	}

	@Override
	public void onHurt(Plugin plugin, Player player, int level, EntityDamageEvent event) {
		double currHealth = player.getHealth();
		double maxHealth = EntityUtils.getMaxHealth(player);
		double hpAfterHit = currHealth - EntityUtils.getRealFinalDamage(event);
		if (currHealth / maxHealth <= HEALTH_LIMIT) {
			event.setDamage(event.getDamage() * Math.pow(1 - DAMAGE_RESIST, level));
		} else if (hpAfterHit / maxHealth <= HEALTH_LIMIT) {
			double hpLostBelowHalf = maxHealth / 2 - hpAfterHit;
			double proportion = hpLostBelowHalf / EntityUtils.getRealFinalDamage(event);
			event.setDamage(event.getDamage() * (1 - proportion) + event.getDamage() * proportion * Math.pow(1 - DAMAGE_RESIST, level));
		}
	}

}
