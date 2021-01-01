package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
/*
 * Curse of Anemia - Reduces all healing received by 10% per level.
 */

public class CurseOfAnemia implements BaseEnchantment {
	public static String PROPERTY_NAME = ChatColor.RED + "Curse of Anemia";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public void onRegain(Plugin plugin, Player player, int level, EntityRegainHealthEvent event) {
		int levelOfReduction = level;
		int sustenanceLevel = plugin.mTrackingManager.mPlayers.getPlayerCustomEnchantLevel(player, Sustenance.class);
		//If player has both Anemia and Sustenance, only one enchant will boost/reduce depending on the higher level.
		if ((sustenanceLevel != 0) && (level - sustenanceLevel > 0)) {
			levelOfReduction = level - sustenanceLevel;
			reduceHealing(plugin, player, levelOfReduction, event);
			//If the player only has Anemia, reduce normally.
		} else if (sustenanceLevel == 0) {
			reduceHealing(plugin, player, level, event);
		}
	}

	public void reduceHealing(Plugin plugin, Player player, int levelOfReduction, EntityRegainHealthEvent event) {
		double reducedHealth;
		//Case if player has over 100% reduced hp, make hp gain 0 instead of losing hp
		if (levelOfReduction >= 10) {
			reducedHealth = 0;
		} else {
			reducedHealth = event.getAmount() * (1 - (0.1 * levelOfReduction));
		}
		event.setAmount(reducedHealth);
	}
}
