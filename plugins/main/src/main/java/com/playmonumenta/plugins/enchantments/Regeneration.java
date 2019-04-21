package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class Regeneration implements BaseEnchantment {
	private static String REGEN = ChatColor.GRAY + "Regeneration";
	private static String MAINHAND_REGEN = ChatColor.GRAY + "Mainhand Regeneration";

	/*
	 * This is only used by the default get level from item implementation
	 * It is garbage here because there's no way to express both Regeneration and Mainhand Regeneration
	 */
	@Override
	public String getProperty() {
		return "THIS IS A BUG";
	}

	@Override
	public int getLevelFromItem(ItemStack item, Player player, ItemSlot slot) {
		if (slot.equals(ItemSlot.MAINHAND)) {
			return InventoryUtils.getCustomEnchantLevel(item, MAINHAND_REGEN);
		} else {
			return InventoryUtils.getCustomEnchantLevel(item, REGEN);
		}
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.ARMOR, ItemSlot.OFFHAND);
	}

	@Override
	public void applyProperty(Plugin plugin, Player player, int level) {
		plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.REGENERATION, 1000049, 0, true, false));
	}

	@Override
	public void removeProperty(Plugin plugin, Player player) {
		plugin.mPotionManager.removePotion(player, PotionID.ITEM, PotionEffectType.REGENERATION);
	}
}
