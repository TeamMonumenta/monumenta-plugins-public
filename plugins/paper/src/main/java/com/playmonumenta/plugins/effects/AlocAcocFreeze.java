package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.EffectTypeApplyFromPotionEvent;
import com.playmonumenta.plugins.hunts.bosses.AlocAcoc;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class AlocAcocFreeze extends ZeroArgumentEffect {
	public static final String effectID = "AlocAcocFreeze";

	public AlocAcocFreeze(int duration) {
		super(duration, effectID);
	}

	@Override
	public void entityApplyEffectTypeFromPotion(Entity entity, EffectTypeApplyFromPotionEvent event) {
		if (event.getItem().getType() == Material.POTION && event.getEffectType().isPositive()) {
			setDuration(0);
		}
	}

	@Override
	public String getDisplayedName() {
		return "Frozen";
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		String display = getDisplayedName();
		if (display == null) {
			return null;
		}
		return Component.text(display, AlocAcoc.COLOR);
	}

	public static AlocAcocFreeze deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new AlocAcocFreeze(duration);
	}
}
