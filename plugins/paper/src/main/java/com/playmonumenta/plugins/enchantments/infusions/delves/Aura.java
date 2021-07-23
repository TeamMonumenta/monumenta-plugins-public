package com.playmonumenta.plugins.enchantments.infusions.delves;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;

public class Aura implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Aura";
	private static final double SLOW_PER_LEVEL = 0.02;
	private static final int DURATION = 10;
	private static final int RADIUS = 3;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void tick(Plugin plugin, Player player, int level) {
		double modifiedLevel = DelveInfusionUtils.getModifiedLevel(plugin, player, level);
		for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), RADIUS)) {
			EntityUtils.applySlow(plugin, DURATION, SLOW_PER_LEVEL * modifiedLevel, mob);
		}
	}
}
