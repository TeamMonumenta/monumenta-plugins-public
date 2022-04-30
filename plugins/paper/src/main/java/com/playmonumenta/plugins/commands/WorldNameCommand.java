package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class WorldNameCommand {

	public static final String TAG = "WorldNamesEnabled";
	public static final String PERMISSION = "monumenta.command.toggleworldnames";

	public static void register() {
		GenericCommand.registerPlayerCommand("toggleworldnames", PERMISSION,
			(sender, player) -> {
				boolean enabled = ScoreboardUtils.toggleTag(player, TAG);
				player.sendMessage(Component.text("World name spoofing is now ", NamedTextColor.GOLD)
					.append(enabled ? Component.text("enabled", NamedTextColor.GREEN) : Component.text("disabled", NamedTextColor.RED))
					.append(Component.text(". You need to switch shards for the change to become effective.", NamedTextColor.GOLD)));
			});
	}

}
