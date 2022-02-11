package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;

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
		double sustenanceLevel = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.SUSTENANCE);
		double anemiaLevel = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.CURSE_OF_ANEMIA);
		double mult = getHealingMultiplier(sustenanceLevel, anemiaLevel);
		// If the player has both Anemia and Sustenance, only one enchant will run to boost/reduce depending on the higher level.
		if (mult > 1) {
			event.setAmount(event.getAmount() * mult);
		}
	}

	public static double getHealingMultiplier(double sustenanceLevel, double anemiaLevel) {
		// If the player has over 100% reduced healing, make hp gain 0 instead of losing hp
		return Math.max(0, 1 + (0.1 * (sustenanceLevel - anemiaLevel)));
	}

}
