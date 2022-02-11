package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.jetbrains.annotations.NotNull;

public class CurseOfAnemia implements Enchantment {

	@Override
	public @NotNull String getName() {
		return "Curse of Anemia";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CURSE_OF_ANEMIA;
	}

	@Override
	public void onRegain(Plugin plugin, Player player, double value, EntityRegainHealthEvent event) {
		double sustenanceLevel = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.SUSTENANCE);
		double anemiaLevel = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.CURSE_OF_ANEMIA);
		double mult = Sustenance.getHealingMultiplier(sustenanceLevel, anemiaLevel);
		// If the player has both Anemia and Sustenance, only one enchant will run to boost/reduce depending on the higher level.
		if (mult < 1) {
			event.setAmount(event.getAmount() * mult);
		}
	}

}
