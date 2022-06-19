package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

public class Quench implements Infusion {

	private static final double DMG_BONUS = 0.01;

	@Override
	public String getName() {
		return "Quench";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.QUENCH;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (hasNonInfPotionEff(player)) {
			double modifiedLevel = DelveInfusionUtils.getModifiedLevel(plugin, player, (int) value);
			event.setDamage(event.getDamage() * (1 + (modifiedLevel * DMG_BONUS)));
		}
	}

	public boolean hasNonInfPotionEff(Player player) {
		for (PotionEffect effect : player.getActivePotionEffects()) {
			if (effect.getDuration() < 80000) {
				return true;
			}
		}
		return false;
	}

}
