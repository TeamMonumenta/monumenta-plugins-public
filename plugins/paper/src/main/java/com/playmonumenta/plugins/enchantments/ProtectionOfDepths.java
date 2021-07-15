package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.server.properties.ServerProperties;

public class ProtectionOfDepths implements BaseEnchantment {

	private static final String PROPERTY_NAME = ChatColor.GRAY + "Protection of the Depths";
	private double mReductionPct = 0;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean isMultiLevel() {
		return false;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void onHurt(Plugin plugin, Player player, int level, EntityDamageEvent event) {
		if (ServerProperties.getClassSpecializationsEnabled() == true) {
			mReductionPct = .25; //25% reduction for region2
		} else {
			mReductionPct = .15; //15% reduction for region 1
		}
		event.setDamage(event.getDamage() * (1.0 - mReductionPct));
	}
}
