package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

public class DurabilitySaving extends SingleArgumentEffect {
	public static final String effectID = "DurabilitySaving";
	public static final String GENERIC_NAME = "DurabilitySaving";

	public DurabilitySaving(int duration, double amount) {
		super(duration, amount, effectID);
	}

	@Override
	public void onDurabilityDamage(Player player, PlayerItemDamageEvent event) {
		if (FastUtils.RANDOM.nextDouble() < mAmount) {
			event.setCancelled(true);
		}
	}

	public static DurabilitySaving deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();

		return new DurabilitySaving(duration, amount);
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount) + " Durability Saving";
	}

	@Override
	public String toString() {
		return String.format("DurabilitySaving duration:%d amount:%f", getDuration(), mAmount);
	}
}
