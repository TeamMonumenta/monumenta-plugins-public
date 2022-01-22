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
	public void onRegain(@NotNull Plugin plugin, @NotNull Player player, double value, @NotNull EntityRegainHealthEvent event) {
		double levelOfReduction = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.CURSE_OF_ANEMIA);
		double sustenanceLevel = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.SUSTENANCE);
		//If player has both Anemia and Sustenance, only one enchant will boost/reduce depending on the higher level.
		if ((sustenanceLevel != 0) && (levelOfReduction - sustenanceLevel > 0)) {
			levelOfReduction = levelOfReduction - sustenanceLevel;
			reduceHealing(plugin, player, (int) levelOfReduction, event);
			//If the player only has Anemia, reduce normally.
		} else if (sustenanceLevel == 0) {
			reduceHealing(plugin, player, (int) levelOfReduction, event);
		}
	}

	public void reduceHealing(Plugin plugin, Player player, int levelOfReduction, EntityRegainHealthEvent event) {
		double reducedHealth;
		//Case if player has over 100% reduced hp, make hp gain 0 instead of losing hp
		if (levelOfReduction >= 10) {
			reducedHealth = 0;
		} else {
			reducedHealth = event.getAmount() * (1 - (0.1 * levelOfReduction));
		}
		event.setAmount(reducedHealth);
	}
}
