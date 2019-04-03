package com.playmonumenta.plugins.utils;

import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

public class AbilityUtils {

	private static final int BOW_MASTER_1_DAMAGE = 3;
	private static final int BOW_MASTER_2_DAMAGE = 6;

	public static int getBowMasteryDamage(Player player) {
		int bowMastery = ScoreboardUtils.getScoreboardValue(player, "BowMastery");
		if (bowMastery > 0) {
			int bonusDamage = bowMastery == 1 ? BOW_MASTER_1_DAMAGE : BOW_MASTER_2_DAMAGE;
			return bonusDamage;
		}
		return 0;
	}

	private static ItemStack getAlchemistPotion() {
		ItemStack stack = new ItemStack(Material.SPLASH_POTION, 1);

		PotionMeta meta = (PotionMeta)stack.getItemMeta();
		meta.setBasePotionData(new PotionData(PotionType.MUNDANE));
		meta.setColor(Color.WHITE);
		meta.setDisplayName(ChatColor.AQUA + "Alchemist's Potion");
		List<String> lore = Arrays.asList(new String[] {
			ChatColor.GRAY + "A unique potion for Alchemists",
		});
		meta.setLore(lore);
		stack.setItemMeta(meta);
		return stack;
	}

	public static void addAlchemistPotions(Player player, int numAddedPotions) {
		if (numAddedPotions == 0) {
			return;
		}

		Inventory inv = player.getInventory();
		ItemStack firstFoundPotStack = null;
		int potCount = 0;

		for (ItemStack item : inv.getContents()) {
			if (InventoryUtils.testForItemWithName(item, "Alchemist's Potion")) {
				if (firstFoundPotStack == null) {
					firstFoundPotStack = item;
				}
				potCount += item.getAmount();
			}
		}

		if (potCount < 32) {
			if (firstFoundPotStack != null) {
				firstFoundPotStack.setAmount(firstFoundPotStack.getAmount() + numAddedPotions);
			} else {
				ItemStack newPotions = getAlchemistPotion();
				newPotions.setAmount(numAddedPotions);
				inv.addItem(newPotions);
			}
		}
	}

}
