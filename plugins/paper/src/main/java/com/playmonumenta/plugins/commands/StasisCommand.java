package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Stasis;
import org.bukkit.entity.Player;

public class StasisCommand extends GenericCommand {

	public static void register() {
		registerPlayerCommand("stasis", "monumenta.command.stasis",
				(sender, player) -> {
					run(player);
				});
	}

	private static void run(Player entity) {
		Plugin.getInstance().mEffectManager.addEffect(entity, Stasis.GENERIC_NAME, new Stasis(120));
	}

}
