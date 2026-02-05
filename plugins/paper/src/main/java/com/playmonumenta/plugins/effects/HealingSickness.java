package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

public class HealingSickness extends ZeroArgumentEffect {
	public static final String effectID = "HealingSickness";


	public HealingSickness(int duration) {
		super(duration, effectID);
	}

	@Override
	public double getMagnitude() {
		// This is useful so that the "active" effect is always the correct duration
		return getDuration();
	}

	@Override
	public boolean isDebuff() {
		return true;
	}

	public static HealingSickness deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new HealingSickness(duration);
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
		return "Healing Sickness";
	}

	@Override
	public String toString() {
		return String.format("Healing Sickness duration:%d", this.getDuration());
	}
}
