package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class Regeneration implements BaseEnchantment {
	private static String REGEN = ChatColor.GRAY + "Regeneration";
	private static String MAINHAND_REGEN = ChatColor.GRAY + "Mainhand Regeneration";
	private static final double BASE_HEAL_RATE = 1.0 / 3 / 4; // Divide by 4 because tick() triggers 4 times a second

	private Map<UUID, Double> mRegenerationTracker = new HashMap<UUID, Double>();

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
			return InventoryUtils.getCustomEnchantLevel(item, MAINHAND_REGEN, useEnchantLevels());
		} else {
			return InventoryUtils.getCustomEnchantLevel(item, REGEN, useEnchantLevels());
		}
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.ARMOR, ItemSlot.OFFHAND);
	}

	// This seems to trigger 4 times a second despite BaseEnchantment claiming it triggers once a second
	@Override
	public void tick(Plugin plugin, World world, Player player, int level) {
		if (!mRegenerationTracker.containsKey(player.getUniqueId())) {
			mRegenerationTracker.put(player.getUniqueId(), 0.0);
		} else {
			double newAmount = mRegenerationTracker.get(player.getUniqueId()) + BASE_HEAL_RATE * Math.sqrt(level);
			if (newAmount >= 1) {
				PlayerUtils.healPlayer(player, 1);
				newAmount--;
			}

			mRegenerationTracker.put(player.getUniqueId(), newAmount);
		}
	}
}
