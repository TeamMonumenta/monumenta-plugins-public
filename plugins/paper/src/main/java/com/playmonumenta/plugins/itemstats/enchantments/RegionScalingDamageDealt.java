package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class RegionScalingDamageDealt implements Enchantment {

	public static final double DAMAGE_DEALT_MULTIPLIER = 0.5;

	@Override
	public String getName() {
		return "RegionScalingDamageDealt";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.REGION_SCALING_DAMAGE_DEALT;
	}

	@Override
	public double getPriorityAmount() {
		return 5000; // should be the final damage dealt modifier
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (!ServerProperties.getClassSpecializationsEnabled()) {
			event.setDamage(event.getDamage() * DAMAGE_DEALT_MULTIPLIER);
		}
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		if (!ServerProperties.getClassSpecializationsEnabled()) {
			plugin.mPotionManager.addPotion(player, PotionManager.PotionID.ITEM,
				new PotionEffect(PotionEffectType.SLOW_DIGGING, 20, 0, false, false, false));
		}
	}
}
