package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class Orbital implements Infusion {
	public static final double DAMAGE_REDUCTION_PER_LEVEL = 0.015;
	private static final double DOWNWARD_SPEED = 0.25;

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.ORBITAL;
	}

	@Override
	public String getName() {
		return "Orbital";
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (source != null && !source.isOnGround()) {
			event.updateDamageWithMultiplier(getDamageTakenMultiplier(value));
			Vector velocity = source.getVelocity();
			velocity.setY(velocity.getY() - DOWNWARD_SPEED);
			source.setVelocity(velocity);
		}
	}

	public static double getDamageTakenMultiplier(double level) {
		return 1 - DAMAGE_REDUCTION_PER_LEVEL * level;
	}
}
