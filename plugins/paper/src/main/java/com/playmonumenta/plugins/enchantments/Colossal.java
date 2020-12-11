package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemDamageEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class Colossal implements BaseEnchantment {
	public static final String PROPERTY_NAME = ChatColor.GRAY + "Colossal";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean useEnchantLevels() {
		return false;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public void onItemDamage(Plugin plugin, Player player, PlayerItemDamageEvent event, int level) {
		if (event.getDamage() > 0) {
			//Check if the item taking damage is the one with the enchantment
			if (InventoryUtils.getCustomEnchantLevel(event.getItem(), PROPERTY_NAME, false) > 0) {
				//With enchant, 50% chance to take durability damage on this event, stacks with unbreaking
				if (FastUtils.RANDOM.nextInt(2) == 0) {
					event.setDamage(0);
				}
			}
		}
	}
}
