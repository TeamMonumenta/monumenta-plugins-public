package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PercentExperience extends SingleArgumentEffect {
	public static final String effectID = "PercentExperience";
	public static final String GENERIC_NAME = "PercentExperience";

	public PercentExperience(int duration, double amount) {
		super(duration, amount, effectID);
	}

	@Override
	public void onExpChange(Player player, PlayerExpChangeEvent event) {
		event.setAmount((int) (event.getAmount() * (1 + mAmount)));
	}

	public static PercentExperience deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();

		return new PercentExperience(duration, amount);
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount) + " Experience Gain";
	}

	@Override
	public String toString() {
		return String.format("PercentExperience duration:%d amount:%f", getDuration(), mAmount);
	}
}
