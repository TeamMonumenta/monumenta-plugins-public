package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.ArrowConsumeEvent;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ArrowSaving extends SingleArgumentEffect {
	public static final String GENERIC_NAME = "ArrowSaving";
	public static final String effectID = "ArrowSaving";

	public ArrowSaving(int duration, double amount) {
		super(duration, amount, effectID);
	}

	@Override
	public void onConsumeArrow(Player player, ArrowConsumeEvent event) {
		if (FastUtils.RANDOM.nextDouble() < mAmount) {
			event.setCancelled(true);
		}
	}

	public static ArrowSaving deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();

		return new ArrowSaving(duration, amount);
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount) + " Arrow Saving";
	}

	@Override
	public String toString() {
		return String.format("ArrowSaving duration:%d amount:%f", getDuration(), mAmount);
	}
}
