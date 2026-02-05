package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

public class AbsorptionSickness extends ZeroArgumentEffect {
	public static final String effectID = "AbsorptionSickness";

	public AbsorptionSickness(int duration) {
		super(duration, effectID);
		mDuration = duration;
	}

	@Override
	public double getMagnitude() {
		return getDuration();
	}

	@Override
	public boolean isDebuff() {
		return true;
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);

		return object;
	}

	public static AbsorptionSickness deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new AbsorptionSickness(duration);
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		String displayedName = getDisplayedName();
		if (displayedName == null) {
			return null;
		}
		return Component.text(displayedName, NamedTextColor.RED);
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Absorption Sickness";
	}

	@Override
	public String toString() {
		return String.format("Absorption Sickness duration:%d", this.getDuration());
	}
}
