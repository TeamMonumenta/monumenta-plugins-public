package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

public class GiftOfTheStars extends ZeroArgumentEffect {
	public static final String GENERIC_NAME = "GiftOfTheStars";
	public static final String effectID = "GiftOfTheStars";
	private static final double REDUCTION = 0.5;

	public GiftOfTheStars(int duration) {
		super(duration, effectID);
	}

	@Override
	public void onKill(EntityDeathEvent event, Player player) {
		if (EntityUtils.isElite(event.getEntity())) {
			PotionUtils.reduceNegatives(Plugin.getInstance(), player, REDUCTION);
		}
	}

	@Override
	public String toString() {
		return String.format("GiftOfTheStars duration:%d", this.getDuration());
	}

	public static GiftOfTheStars deserialize(JsonObject object) {
		int duration = object.get("duration").getAsInt();
		return new GiftOfTheStars(duration);
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Gift of the Stars";
	}
}
