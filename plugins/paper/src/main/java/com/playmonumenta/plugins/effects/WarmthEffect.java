package com.playmonumenta.plugins.effects;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

// Reduce incoming damage based on charges
public class WarmthEffect extends Effect {

	public WarmthEffect(int duration) {
		super(duration);
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz && entity instanceof Player player) {
			player.setFoodLevel(Math.min(20, player.getFoodLevel() + 1));
			player.setSaturation(Math.min(player.getFoodLevel(), Math.min(player.getSaturation() + 1, 20)));
		}
	}

	@Override
	public String toString() {
		return String.format("WarmthEffect duration:%d", this.getDuration());
	}
}
