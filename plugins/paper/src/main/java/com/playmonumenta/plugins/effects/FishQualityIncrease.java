package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class FishQualityIncrease extends SingleArgumentEffect {
	public static final String effectID = "FishQualityIncrease";

	public FishQualityIncrease(int duration, double amount) {
		super(duration, amount, effectID);
		displays(ServerProperties.getShardName().contains("ring"));
	}

	@Override
	public double getFishQualityIncrease(Player player) {
		return 1 + mAmount;
	}

	@Override
	public boolean isBuff() {
		return mAmount > 0;
	}

	@Override
	public boolean isDebuff() {
		return mAmount < 0;
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount).append(Component.text(" " + getDisplayedName()));
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Fish Quality";
	}

	public static FishQualityIncrease deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();

		return new FishQualityIncrease(duration, amount);
	}

	@Override
	public String toString() {
		return String.format("FishQualityIncrease duration:%d amount:%f", getDuration(), mAmount);
	}
}
