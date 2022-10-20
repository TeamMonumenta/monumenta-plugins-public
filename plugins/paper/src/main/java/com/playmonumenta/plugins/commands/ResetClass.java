package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.AbilityUtils;

public class ResetClass extends GenericCommand {
	private static final String PERMISSION = "monumenta.command.resetclass";

	public static void register(Plugin plugin) {
		registerPlayerCommand("resetclass", PERMISSION,
			(sender, player) -> {
				AbilityUtils.resetClass(player);
			});

		registerPlayerCommand("resetspec", PERMISSION,
			(sender, player) -> {
				AbilityUtils.resetSpec(player);
			});
	}
}
