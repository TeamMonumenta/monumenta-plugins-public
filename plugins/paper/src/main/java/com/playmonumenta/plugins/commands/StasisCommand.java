package com.playmonumenta.plugins.commands;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Stasis;

public class StasisCommand extends GenericCommand {

	public static void register() {
		registerPlayerCommand("stasis", "monumenta.command.stasis",
				(sender, player) -> {
					run(player);
				});
	}

	private static void run(Player entity) {
		Plugin.getInstance().mEffectManager.addEffect(entity, Stasis.STASIS_NAME, new Stasis(120));
	}

}
