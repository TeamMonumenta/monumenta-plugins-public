package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EarthAspect implements Enchantment {

	private static String EARTH_STRING = "EarthAspect";
	private static int DURATION = 3 * 20;
	private static double DAMAGE_REDUCTION_PER_LEVEL = -0.05;

	@Override
	public @NotNull String getName() {
		return "Earth Aspect";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.EARTH_ASPECT;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		DamageEvent.DamageType type = event.getType();
		if ((type == DamageEvent.DamageType.MELEE && ItemStatUtils.isNotExclusivelyRanged(player.getInventory().getItemInMainHand())) || type == DamageEvent.DamageType.PROJECTILE) {
			plugin.mEffectManager.addEffect(player, EARTH_STRING, new PercentDamageReceived(DURATION, DAMAGE_REDUCTION_PER_LEVEL * level));
		}
	}

}
