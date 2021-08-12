package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.utils.PlayerUtils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class PercentRegeneration extends Effect {

	private final double mAmount;

	public PercentRegeneration(int duration, double amount) {
		super(duration);
		mAmount = amount;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz) {
			if (entity instanceof Player) {
				PlayerUtils.healPlayer((Player)entity, mAmount);
			}
		}
	}

	@Override
	public String toString() {
		return String.format("PercentHealing duration:%d amount:%f", this.getDuration(), mAmount);
	}

}
