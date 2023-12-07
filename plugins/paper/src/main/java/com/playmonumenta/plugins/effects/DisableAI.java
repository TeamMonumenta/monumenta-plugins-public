package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class DisableAI extends ZeroArgumentEffect {
	public static final String effectID = "DisableAI";

	private boolean mStartedWithAI = false;

	public DisableAI(int duration) {
		super(duration, effectID);
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof LivingEntity le) {
			mStartedWithAI = le.hasAI();
			le.setAI(false);
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof LivingEntity le && mStartedWithAI) {
			le.setAI(true);
		}
	}

	public static DisableAI deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new DisableAI(duration);
	}

	@Override
	public String toString() {
		return String.format("DisableAI duration: %s", mDuration);
	}
}
