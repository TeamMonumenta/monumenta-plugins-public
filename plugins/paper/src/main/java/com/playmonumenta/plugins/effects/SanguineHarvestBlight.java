package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Similar to Vulnerability,
 * Blight is not considered a debuff itself but increases damage dealt to the entity based
 * on the amount of debuffs the entity has.
 */
public class SanguineHarvestBlight extends Effect {
	public static final String effectID = "SanguineHarvestBlight";
	public static final String GENERIC_NAME = "SanguineHarvestBlightEffect";

	private final double mAmount;
	private final Plugin mPlugin;

	public SanguineHarvestBlight(int duration, double amount, Plugin plugin) {
		super(duration, effectID);
		mAmount = amount;
		mPlugin = plugin;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		event.setDamage(event.getDamage() * (1 + mAmount * AbilityUtils.getDebuffCount(mPlugin, entity)));
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(-mAmount) + " Resistance Per Debuff";
	}

	@Override
	public String toString() {
		return String.format("SanguineHarvestBlight duration:%d amount:%f", this.getDuration(), mAmount);
	}
}
