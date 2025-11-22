package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.abilities.windwalker.Whirlwind;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import org.bukkit.entity.Player;

public class DepthsWinded extends Effect {
	public static final String effectID = "DepthsWinded";

	private static final int DURATION = 20 * 60;

	public DepthsWinded() {
		this(DURATION);
	}

	public DepthsWinded(int duration) {
		super(duration, effectID);
	}

	@Override
	public void onAbilityCast(AbilityCastEvent event, Player player) {
		Whirlwind whirlwind = Plugin.getInstance().mAbilityManager.getPlayerAbility(player, Whirlwind.class);
		if (whirlwind != null) {
			whirlwind.trigger(event.getAbility());
			clearEffect();
		}
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		return object;
	}

	public static DepthsWinded deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		return new DepthsWinded(duration);
	}

	@Override
	public String toString() {
		return String.format("%s duration:%d", effectID, mDuration);
	}
}
