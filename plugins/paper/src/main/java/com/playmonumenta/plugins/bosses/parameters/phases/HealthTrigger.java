package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import dev.jorel.commandapi.Tooltip;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class HealthTrigger extends Trigger {

	private final double mHealthPercentage;

	private HealthTrigger(double health) {
		mHealthPercentage = health;
	}

	@Override public boolean test(LivingEntity boss) {
		double maxHealth = EntityUtils.getMaxHealth(boss);
		double currentHealth = boss.getHealth();

		return currentHealth / maxHealth <= mHealthPercentage;
	}

	@Override public void reset(LivingEntity boss) {

	}

	@Override
	public boolean onHurt(LivingEntity boss, @Nullable LivingEntity damager, DamageEvent event) {
		double maxHealth = EntityUtils.getMaxHealth(boss);
		double currentHealth = boss.getHealth();

		return (currentHealth - event.getFinalDamage(true)) / maxHealth <= mHealthPercentage;
	}

	public static ParseResult<Trigger> fromReader(StringReader reader) {
		if (!reader.advance("(")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "(", "(..)")));
		}
		Double health = reader.readDouble();
		if (health == null || health < 0) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "0.9", "range must be positive")));
		}

		if (!reader.advance(")")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ")", "(..)")));
		}

		return ParseResult.of(new HealthTrigger(health));
	}
}
