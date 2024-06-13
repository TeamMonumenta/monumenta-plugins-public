package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class GUITextures {

	public static void register() {

		new CommandAPICommand("GUITextures")
			.withPermission("monumenta.guitextures")
			.withAliases("guit")
			.executesPlayer((player, args) -> {
				if (ScoreboardUtils.getScoreboardValue(player, GUIUtils.GUI_TEXTURES_OBJECTIVE).orElse(0) == 0) {
					ScoreboardUtils.setScoreboardValue(player, GUIUtils.GUI_TEXTURES_OBJECTIVE, 1);
					player.sendMessage(Component.text("Resource Pack GUI textures have been ", NamedTextColor.GOLD).append(Component.text("disabled", NamedTextColor.RED).append(Component.text(".", NamedTextColor.GOLD))));
				} else {
					ScoreboardUtils.setScoreboardValue(player, GUIUtils.GUI_TEXTURES_OBJECTIVE, 0);
					player.sendMessage(Component.text("Resource Pack GUI textures have been ", NamedTextColor.GOLD).append(Component.text("enabled", NamedTextColor.GREEN).append(Component.text(".", NamedTextColor.GOLD))));
				}
			})
			.register();
	}
}
