package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

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
		int[] slotList = new int[]{inv.getHeldItemSlot(), 36, 37, 38, 39, 40};
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
		int repairs = binomialDistributionRoll(xpAmount, PERCENT_CHANCE * value);
		if (repairs > 0) {
			for (int i : slotList) {
				ItemStack item = inv.getItem(i);
				if (item == null || !item.hasItemMeta()) {
					continue;
				}
				Material type = item.getType();
				if (type == Material.AIR || type.isBlock() || ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.CURSE_OF_IRREPARIBILITY) > 0) {
					continue;
				}
				ItemMeta meta = item.getItemMeta();
				if (meta instanceof Damageable damageMeta) {
					int currDura = damageMeta.getDamage();
					if (currDura <= 0) {
						continue;
					}
					int maxDura = type.getMaxDurability();
					double newDura = Math.max(currDura - (maxDura * repairs * PERCENT_REPAIR), 0);
					double frac = newDura - Math.floor(newDura);
					if (Math.random() < frac) {
						newDura = Math.floor(newDura);
					} else {
						newDura = Math.ceil(newDura);
					}
					damageMeta.setDamage((short) newDura);
					item.setItemMeta(damageMeta);
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
