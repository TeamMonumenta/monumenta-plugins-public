package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class VirtualFirmament {

	private static final String TAG = "VirtualFirmament";

	public static void register() {

		new CommandAPICommand("virtualfirmament")
			.withPermission("monumenta.virtualfirmament")
			.withAliases("vf")
			.executesPlayer((player, args) -> {
				boolean enabled = ScoreboardUtils.toggleTag(player, TAG);
				player.sendMessage(Component.text("Virtual Firmament ", NamedTextColor.GOLD).append(enabled ? Component.text("enabled", NamedTextColor.GREEN) : Component.text("disabled", NamedTextColor.RED)));
				player.updateInventory();
			})
			.register();

	}

	public static boolean isEnabled(Player player) {
		return ScoreboardUtils.checkTag(player, TAG);
	}

}
