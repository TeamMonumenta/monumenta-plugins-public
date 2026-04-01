package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.ZoneUtils;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Frogging implements Enchantment {

	private static final int DURATION = 20;

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.FROGGING;
	}

	@Override
	public String getName() {
		return "Frogging";
	}

	@Override
	public void tick(Plugin plugin, Player player, double level, boolean twoHertz, boolean oneHertz) {
		if (!ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
			plugin.mPotionManager.addPotion(player, PotionManager.PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, DURATION, (int) level - 1, true, true));
		}
	}
}
