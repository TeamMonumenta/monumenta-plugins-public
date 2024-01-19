package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.EntityGainAbsorptionEvent;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public class PercentAbsorption extends SingleArgumentEffect {
	public static final String effectID = "PercentAbsorption";
	public static final String GENERIC_NAME = "PercentAbsorption";

	public PercentAbsorption(int duration, double amount) {
		super(duration, amount, effectID);
	}

	@Override
	public boolean isDebuff() {
		return mAmount < 0;
	}

	@Override
	public boolean isBuff() {
		return mAmount > 0;
	}

	@Override
	public void entityGainAbsorptionEvent(EntityGainAbsorptionEvent event) {
		event.setAmount(event.getAmount() * (1 + mAmount));
		event.setMaxAmount(event.getMaxAmount() * (1 + mAmount));
	}

	public double getValue() {
		return mAmount;
	}

	public static PercentAbsorption deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();

		return new PercentAbsorption(duration, amount);
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount).append(Component.text(" " + getDisplayedName()));
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Absorption";
	}

	@Override
	public String toString() {
		return String.format("PercentAbsorption duration:%d amount:%f", this.getDuration(), mAmount);
	}
}
