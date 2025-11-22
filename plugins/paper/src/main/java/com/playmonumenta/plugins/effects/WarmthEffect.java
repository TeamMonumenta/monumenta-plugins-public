package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

// Over the duration, increases food and saturation values of player every 1 second.
public final class WarmthEffect extends Effect {
	public static final String effectID = "WarmthEffect";

	private final float mAmount;
	private double mRemainder;

	public WarmthEffect(final int duration, final float amount) {
		super(duration, effectID);
		mAmount = amount;
	}

	@Override
	public void entityTickEffect(final Entity entity, final boolean fourHertz, final boolean twoHertz, final boolean oneHertz) {
		if (oneHertz && entity instanceof final Player player) {
			PlayerUtils.addFoodLevel(player, (int) Math.floor(mAmount));
			PlayerUtils.addFoodSaturationLevel(player, mAmount);

			// Getting around integer limitations!
			mRemainder += mAmount % 1;
			if (mRemainder > 1) {
				PlayerUtils.addFoodLevel(player, 1);
				mRemainder--;
			}
		}
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public JsonObject serialize() {
		final JsonObject object = new JsonObject();

		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);

		return object;
	}

	public static WarmthEffect deserialize(final JsonObject object, final Plugin plugin) {
		final int duration = object.get("duration").getAsInt();
		final float amount = object.get("amount").getAsFloat();

		return new WarmthEffect(duration, amount);
	}

	@Override
	public String getDisplayedName() {
		return "Intoxicating Warmth";
	}

	@Override
	public String toString() {
		return String.format("WarmthEffect duration:%d", this.getDuration());
	}
}
