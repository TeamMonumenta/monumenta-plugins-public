package com.playmonumenta.plugins.enchantments.infusions.delves;

import java.util.EnumSet;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

public class Empowered implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Empowered";
	private static final double PERCENT_CHANCE = 0.01;
	private static final double PERCENT_REPAIR = 0.01;
	private static final String ACUMEN_TAG = ChatColor.GRAY + "Acumen";
	private static final double ACUMEN_BONUS = 0.02;
	private static final String INTUITION_TAG = ChatColor.GRAY + "Intuition";
	private static final double INTUITION_BONUS = 0.5;

	@Override
	public @NotNull String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void onExpChange(Plugin plugin, Player player, PlayerExpChangeEvent playerExpChangeEvent, int level) {
		float acumenBonus = 1.0f;
		float intuitionBonus = 1.0f;
		boolean intuitionCheck = true;

		PlayerInventory inv = player.getInventory();
		int[] slotList = new int[] {inv.getHeldItemSlot(), 36, 37, 38, 39, 40};
		for (int i : slotList) {
			ItemStack item = inv.getItem(i);
			int acumen = InventoryUtils.getCustomEnchantLevel(item, ACUMEN_TAG, true);
			int intuition = InventoryUtils.getCustomEnchantLevel(item, INTUITION_TAG, true);
			if (intuition > 0 && intuitionCheck) {
				intuitionBonus += INTUITION_BONUS;
				intuitionCheck = false;
			}
			if (acumen > 0) {
				acumenBonus += acumen * ACUMEN_BONUS;
			}
		}

		int xpAmount = (int)(playerExpChangeEvent.getAmount() * intuitionBonus * acumenBonus);
		int repairs = binomialDistributionRoll(xpAmount, PERCENT_CHANCE * DelveInfusionUtils.getModifiedLevel(plugin, player, level));
		if (repairs > 0) {
			for (int i : slotList) {
				ItemStack item = inv.getItem(i);
				if ((item != null && item.getDurability() > 0 && !item.getType().isBlock()
					    && (!item.hasItemMeta() || !item.getItemMeta().hasLore()
					        || (!InventoryUtils.testForItemWithLore(item, "* Irreparable *")
					            && !InventoryUtils.testForItemWithLore(item, "Curse of Irreparability"))))) {
					int maxDura = item.getType().getMaxDurability();
					int currDura = item.getDurability();
					double newDura = Math.max(currDura - (maxDura * repairs * PERCENT_REPAIR), 0);
					item.setDurability((short)newDura);
				}
			}
		}
	}

	private int binomialDistributionRoll(int n, double p) {
		int hits = 0;
		for (int i = 0; i < n; i++) {
			if (Math.random() <= p) {
				hits++;
			}
		}
		return hits;
	}
}
