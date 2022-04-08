package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PercentExperience extends SingleArgumentEffect {
	public static final String GENERIC_NAME = "PercentExperience";

	public PercentExperience(int duration, double amount) {
		super(duration, amount);
	}

	@Override
	public void onExpChange(Player player, PlayerExpChangeEvent event) {
		event.setAmount((int) (event.getAmount() * (1 + mAmount)));
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
