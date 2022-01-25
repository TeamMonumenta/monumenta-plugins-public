package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.InfernoDamage;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.NavigableSet;

public class Inferno implements Enchantment {

	@Override
	public String getName() {
		return "Inferno";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.INFERNO;
	}

	//Called in EntityUtils.applyFire()
	public static void apply(Plugin plugin, Player player, int level, LivingEntity enemy, int duration) {
		plugin.mEffectManager.addEffect(enemy, "Inferno", new InfernoDamage(duration, level, player));
	}

	public static boolean hasInferno(Plugin plugin, LivingEntity mob) {
		NavigableSet<Effect> effects = plugin.mEffectManager.getEffects(mob, "Inferno");
		if (effects != null) {
			return true;
		}
		return false;
	}

	public static int getInfernoLevel(Plugin plugin, LivingEntity mob) {
		NavigableSet<Effect> effects = plugin.mEffectManager.getEffects(mob, "Inferno");
		if (effects != null) {
			Effect effect = effects.last();
			return (int) effect.getMagnitude();
		} else {
			return 0;
		}
	}
}
