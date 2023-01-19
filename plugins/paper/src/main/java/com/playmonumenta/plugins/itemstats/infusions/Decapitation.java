package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Decapitation implements Infusion {

	public static final double DAMAGE_MLT_PER_LVL = 0.0125;

	@Override public String getName() {
		return "Decapitation";
	}

	@Override public ItemStatUtils.InfusionType getInfusionType() {
		return ItemStatUtils.InfusionType.DECAPITATION;
	}

	@Override
	public double getPriorityAmount() {
		return 23;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE && PlayerUtils.isFallingAttack(player)) {
			event.setDamage(event.getDamage() * getDamageDealtMultiplier(DelveInfusionUtils.getModifiedLevel(plugin, player, (int) value)));
		}
	}

	public static double getDamageDealtMultiplier(double level) {
		return 1 + DAMAGE_MLT_PER_LVL * level;
	}
}
