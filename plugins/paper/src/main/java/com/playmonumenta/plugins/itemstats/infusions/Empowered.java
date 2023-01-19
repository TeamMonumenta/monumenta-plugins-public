package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class Empowered implements Infusion {

	public static final double PERCENT_CHANCE = 0.0025;
	private static final double PERCENT_REPAIR = 0.01;
	private static final float ACUMEN_BONUS = 0.02f;
	private static final float INTUITION_BONUS = 0.5f;

	@Override
	public String getName() {
		return "Empowered";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.EMPOWERED;
	}

	@Override
	public void onExpChange(Plugin plugin, Player player, double value, PlayerExpChangeEvent event) {
		float acumenBonus = 1.0f;
		float intuitionBonus = 1.0f;
		boolean intuitionCheck = true;

		PlayerInventory inv = player.getInventory();
		int[] slotList = new int[] {inv.getHeldItemSlot(), 36, 37, 38, 39, 40};
		for (int i : slotList) {
			ItemStack item = inv.getItem(i);
			int acumen = ItemStatUtils.getInfusionLevel(item, InfusionType.ACUMEN);
			int intuition = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.INTUITION);
			if (intuition > 0 && intuitionCheck) {
				intuitionBonus += INTUITION_BONUS;
				intuitionCheck = false;
			}
			if (acumen > 0) {
				acumenBonus += acumen * ACUMEN_BONUS;
			}
		}

		int xpAmount = (int) (event.getAmount() * intuitionBonus * acumenBonus);
		int repairs = binomialDistributionRoll(xpAmount, PERCENT_CHANCE * DelveInfusionUtils.getModifiedLevel(plugin, player, (int) value));
		if (repairs > 0) {
			for (int i : slotList) {
				ItemStack item = inv.getItem(i);
				if ((item != null && item.getDurability() > 0 && !item.getType().isBlock()
					    && item.hasItemMeta() && ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.CURSE_OF_IRREPARIBILITY) == 0)) {
					int maxDura = item.getType().getMaxDurability();
					int currDura = item.getDurability();
					double newDura = Math.max(currDura - (maxDura * repairs * PERCENT_REPAIR), 0);
					double frac = newDura - Math.floor(newDura);
					if (Math.random() < frac) {
						newDura = Math.floor(newDura);
					} else {
						newDura = Math.ceil(newDura);
					}
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
