package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

//Sustenance - Increases all healing by 10% for each level
public class Sustenance implements BaseEnchantment {
	public static String PROPERTY_NAME = ChatColor.GRAY + "Sustenance";

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
		int levelOfBoost = level;
		int anemiaLevel = plugin.mTrackingManager.mPlayers.getPlayerCustomEnchantLevel(player, CurseOfAnemia.class);
		//If player has both Anemia and Sustenance, only one enchant will run to boost/reduce depending on the higher level.
		if ((anemiaLevel != 0) && (level - anemiaLevel > 0)) {
			levelOfBoost = level - anemiaLevel;
			boostHealing(plugin, player, levelOfBoost, event);
			//If player has only Sustanance, just boost normally.
		} else if (anemiaLevel == 0) {
			boostHealing(plugin, player, level, event);
		}
	}

	public void boostHealing(Plugin plugin, Player player, int levelOfBoost, EntityRegainHealthEvent event) {
		double boostedHealth = event.getAmount() * (1 + (0.1 * levelOfBoost));
		event.setAmount(boostedHealth);
	}
}
