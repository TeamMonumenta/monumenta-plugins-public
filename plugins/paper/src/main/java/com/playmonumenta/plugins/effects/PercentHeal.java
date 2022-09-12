package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PercentHeal extends SingleArgumentEffect {
	public static final String effectID = "PercentHeal";

	public PercentHeal(int duration, double amount) {
		super(duration, amount, effectID);
	}

	@Override
	public boolean isDebuff() {
		return mAmount < 0;
	}

	@Override
	public boolean entityRegainHealthEvent(EntityRegainHealthEvent event) {
		event.setAmount(event.getAmount() * (1 + mAmount));
		return mAmount > -1;
	}

	public static PercentHeal deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();

		return new PercentHeal(duration, amount);
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount) + " Healing";
	}

	@Override
	public String toString() {
		return String.format("PercentHeal duration:%d amount:%f", this.getDuration(), mAmount);
	}
}
