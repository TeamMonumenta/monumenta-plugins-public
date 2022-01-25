package com.playmonumenta.plugins.itemstats.enchantments;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;

public class Darksight implements Enchantment {

	private static final String DARKSIGHT_DISABLED_TAG = "DarksightDisabled";

	@Override
	public String getName() {
		return "Darksight";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.DARKSIGHT;
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		if (oneHz) {
			if (!player.getScoreboardTags().contains(DARKSIGHT_DISABLED_TAG)) {
				plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 10, 0, true, false));
			}
		}
	}
}
