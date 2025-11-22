package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class HealthTrigger extends Trigger {
	public static final String IDENTIFIER = "HEALTH";

	private final double mHealthPercentage;

	public HealthTrigger(double health) {
		mHealthPercentage = health;
	}

	@Override
	public boolean test(LivingEntity boss) {
		double maxHealth = EntityUtils.getMaxHealth(boss);
		double currentHealth = boss.getHealth();

		return currentHealth / maxHealth <= mHealthPercentage;
	}

	@Override
	public void reset(LivingEntity boss) {

	}

	@Override
	public boolean onHurt(LivingEntity boss, @Nullable LivingEntity damager, DamageEvent event) {
		double maxHealth = EntityUtils.getMaxHealth(boss);
		double currentHealth = boss.getHealth();

		return (currentHealth - event.getFinalDamage(true)) / maxHealth <= mHealthPercentage;
	}

}
