package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import org.bukkit.entity.Entity;

public class DisableGravity extends ZeroArgumentEffect {
	public static final String effectID = "DisableGravity";

	private boolean mStartedWithGravity = false;

	public DisableGravity(int duration) {
		super(duration, effectID);
	}

	@Override
	public void entityGainEffect(Entity entity) {
		mStartedWithGravity = entity.hasGravity();
		entity.setGravity(false);
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (mStartedWithGravity) {
			entity.setGravity(true);
		}
	}

	public static DisableGravity deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new DisableGravity(duration);
	}

	@Override
	public String toString() {
		return String.format("DisableGravity duration: %s", mDuration);
	}
}
