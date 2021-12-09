package com.playmonumenta.plugins.enchantments.infusions.delves;

import java.util.EnumSet;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;


public class Usurper implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Usurper";
	private static final double HEAL_PCT_PER_LVL = 0.025;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void onKill(Plugin plugin, Player player, int level, Entity enemy, EntityDeathEvent entityDeathEvent) {
		if (EntityUtils.isBoss(enemy) || EntityUtils.isElite(enemy)) {
			double healAmount = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * HEAL_PCT_PER_LVL * DelveInfusionUtils.getModifiedLevel(plugin, player, level);
			PlayerUtils.healPlayer(player, healAmount);
		}
	}

}
