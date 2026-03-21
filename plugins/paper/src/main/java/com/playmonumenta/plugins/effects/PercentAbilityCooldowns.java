package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class PercentAbilityCooldowns extends SingleArgumentEffect {
	public static final String GENERIC_NAME = "PercentAbilityCooldown";
	public static final String effectID = "PercentAbilityCooldown";

	public PercentAbilityCooldowns(int duration, double amount) {
		super(duration, amount, effectID);
	}

	public static PercentAbilityCooldowns deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();

		return new PercentAbilityCooldowns(duration, amount);
	}

	@Override
	public void onAbilityCast(AbilityCastEvent event, Player player) {
		event.setCooldown((int) (event.getCooldown() * (1 + mAmount)));
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount, true).append(Component.text(" " + getDisplayedName()));
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Ability Cooldowns";
	}

	@Override
	public String toString() {
		return String.format("PercentAbilityCooldowns duration:%d amount:%f", getDuration(), mAmount);
	}
}
