package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

// Over the duration, increases food and saturation values of player every 1 second.
public class WarmthEffect extends Effect {
	public static final String effectID = "WarmthEffect";

	private final float mAmount;
	private double mRemainder;

	public WarmthEffect(int duration, float amount) {
		super(duration, effectID);
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
	public boolean isBuff() {
		return true;
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();

		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);

		return object;
	}

	public static WarmthEffect deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		float amount = object.get("amount").getAsFloat();

		return new WarmthEffect(duration, amount);
	}

	@Override
	public String toString() {
		return String.format("WarmthEffect duration:%d", this.getDuration());
	}
}
