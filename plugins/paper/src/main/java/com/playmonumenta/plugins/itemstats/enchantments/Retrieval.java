package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;


public class Retrieval implements Enchantment {
	private static final float RETRIEVAL_CHANCE = 0.1f;
	public static final String CHARM_CHANCE = "Retrieval Chance";

	@Override
	public String getName() {
		return "Retrieval";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.RETRIEVAL;
	}

	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double level, ProjectileLaunchEvent event, Projectile proj) {
		double chance = (RETRIEVAL_CHANCE * level) + CharmManager.getLevelPercentDecimal(player, CHARM_CHANCE);
		if ((proj.getType() == EntityType.ARROW || proj.getType() == EntityType.SPECTRAL_ARROW) && FastUtils.RANDOM.nextDouble() < chance) {
			boolean refunded = AbilityUtils.refundArrow(player, (AbstractArrow) proj);
			if (refunded) {
				player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.3f, 1.0f);
			}
		}
	}
}
