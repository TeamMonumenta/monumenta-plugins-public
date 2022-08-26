package com.playmonumenta.plugins.effects;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

// Over the duration, increases food and saturation values of player every 1 second.
public class WarmthEffect extends Effect {

	private final float mAmount;
	private double mRemainder;

	public WarmthEffect(int duration, float amount) {
		super(duration);
		mAmount = amount;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz && entity instanceof Player player) {
			player.setFoodLevel(Math.min(20, player.getFoodLevel() + (int) Math.floor(mAmount)));
			player.setSaturation(Math.min(player.getFoodLevel(), Math.min(player.getSaturation() + mAmount, 20)));

			// Getting around integer limitations!
			mRemainder += mAmount % 1;
			if (mRemainder > 1) {
				player.setFoodLevel(Math.min(20, player.getFoodLevel() + 1));
				mRemainder -= 1;
			}
		}
	}

	@Override
	public String toString() {
		return String.format("WarmthEffect duration:%d", this.getDuration());
	}
}
