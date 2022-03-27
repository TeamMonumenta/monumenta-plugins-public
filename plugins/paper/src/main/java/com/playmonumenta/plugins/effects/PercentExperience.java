package com.playmonumenta.plugins.effects;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;

public class PercentExperience extends SingleArgumentEffect {
	public static final String GENERIC_NAME = "PercentExperience";

	public PercentExperience(int duration, double amount) {
		super(duration, amount);
	}

	@Override
	public void onExpChange(Player player, PlayerExpChangeEvent event) {
		event.setAmount((int) (event.getAmount() * mAmount));
	}

	@Override
	public String toString() {
		return String.format("PercentExperience duration:%d amount:%f", getDuration(), mAmount);
	}
}
