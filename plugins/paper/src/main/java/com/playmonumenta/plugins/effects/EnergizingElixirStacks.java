package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import org.bukkit.ChatColor;
import org.checkerframework.checker.nullness.qual.Nullable;

public class EnergizingElixirStacks extends SingleArgumentEffect {
	public static final String effectID = "EnergizingElixirStacks";
	public static final String GENERIC_NAME = "EnergizingElixirStacks";

	public EnergizingElixirStacks(int duration, double amount) {
		super(duration, amount, effectID);
	}

	public static EnergizingElixirStacks deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();

		return new EnergizingElixirStacks(duration, amount);
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return ChatColor.GREEN + "" + (int) mAmount + " Energizing Elixir Stacks";
	}

	@Override
	public String toString() {
		return String.format("EnergizingElixirStacks duration:%d amount:%f", getDuration(), mAmount);
	}
}
