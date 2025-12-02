package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class PercentPotionRecharge extends Effect {
	public static final String GENERIC_NAME = "PotionRechargeRateMultiplier";
	public static final String effectID = "PotionRechargeRateMultiplier";

	private final double mAmount;
	private final String mMultiplierName;
	private final AlchemistPotions mAlchemistPotions;

	public PercentPotionRecharge(int duration, double amount, String multiplierName, AlchemistPotions alchemistPotions) {
		super(duration, effectID);
		mAmount = amount;
		mMultiplierName = multiplierName;
		mAlchemistPotions = alchemistPotions;
	}

	@Override
	public void entityGainEffect(Entity entity) {
		mAlchemistPotions.addRechargeRateMultiplier(mMultiplierName, 1 + mAmount);
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		mAlchemistPotions.removeRechargeRateMultiplier(mMultiplierName);
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public boolean isBuff() {
		return mAmount > 0;
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "%s%s%% Potion Recharge Rate".formatted(mAmount < 0 ? "-" : "+", StringUtils.to2DP(mAmount * 100));
	}

	public static @Nullable PercentPotionRecharge deserialize(JsonObject object, Plugin plugin) {
		return null;
	}

	@Override
	public String toString() {
		return String.format("PotionRechargeRateMultiplier duration:%d amount:%f", this.getDuration(), mAmount);
	}
}
