package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class Gills implements Enchantment {

	@Override
	public @NotNull String getName() {
		return "Gills";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.GILLS;
	}

	@Override
	public void tick(@NotNull Plugin plugin, @NotNull Player player, double value, boolean twoHz, boolean oneHz) {
		if (oneHz) {
			plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.WATER_BREATHING, 21, 0, true, false));
		}
	}
}
