package com.playmonumenta.plugins.itemstats.enchantments;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;

public class Sustenance implements Enchantment {

	@Override
	public String getName() {
		return "Sustenance";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.SUSTENANCE;
	}

	@Override
	public void onRegain(Plugin plugin, Player player, double value, EntityRegainHealthEvent event) {
		double levelOfBoost = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.SUSTENANCE);
		double anemiaLevel = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.CURSE_OF_ANEMIA);
		//If player has both Anemia and Sustenance, only one enchant will run to boost/reduce depending on the higher level.
		if ((anemiaLevel != 0) && (levelOfBoost - anemiaLevel > 0)) {
			levelOfBoost = levelOfBoost - anemiaLevel;
			boostHealing(plugin, player, (int) levelOfBoost, event);
			//If player has only Sustanance, just boost normally.
		} else if (anemiaLevel == 0) {
			boostHealing(plugin, player, (int) levelOfBoost, event);
		}
	}

	public void boostHealing(Plugin plugin, Player player, int levelOfBoost, EntityRegainHealthEvent event) {
		double boostedHealth = event.getAmount() * (1 + (0.1 * levelOfBoost));
		event.setAmount(boostedHealth);
	}
}
