package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SilverPrayer extends ZeroArgumentEffect {
	public static final String GENERIC_NAME = "SilverPrayer";
	public static final String effectID = "SilverPrayer";

	public static final String EFFECT_NAME = "SilverPrayerKnockbackRes";
	public static final int EFFECT_DURATION = 2 * 20;

	public SilverPrayer(int duration) {
		super(duration, effectID);
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz) {
			if (entity instanceof Player player) {
				if (player.getHealth() / EntityUtils.getMaxHealth(player) <= 0.5) {
					Plugin.getInstance().mEffectManager.addEffect(player, EFFECT_NAME, new PercentKnockbackResist(EFFECT_DURATION, 1, EFFECT_NAME).displaysTime(false));
				}
			}
		}
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	public static SilverPrayer deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new SilverPrayer(duration);
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return "Boon of Silver Prayer";
	}

	@Override
	public String toString() {
		return String.format("SilverPrayer duration:%d", this.getDuration());
	}

}
