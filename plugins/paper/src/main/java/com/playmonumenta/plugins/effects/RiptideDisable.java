package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class RiptideDisable extends ZeroArgumentEffect {
	public static final String effectID = "RiptideDisable";

	public RiptideDisable(int duration) {
		super(duration, effectID);
	}

	public static RiptideDisable deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new RiptideDisable(duration);
	}

	@Override
	public String toString() {
		return String.format("RiptideDisable duration:%d", this.getDuration());
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (entity.isOnGround() || entity.isInWater()) {
			setDuration(0);
			Plugin.getInstance().mPotionManager.clearPotionEffectType((Player) entity, PotionEffectType.SLOW_FALLING);
		}
	}
}
