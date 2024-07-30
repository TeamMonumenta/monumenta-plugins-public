package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class StreamerModeCommand extends TogglePermCommand {
	@Override
	public String getCommandName() {
		return "streamermode";
	}

	@Override
	public String getSetToDescription() {
		return "Turn Streamer Mode on?";
	}

	@Override
	public String getCanTogglePerm() {
		return "monumenta.streamermode";
	}

	@Override
	public String getToggledPerm() {
		return "group.streamermode";
	}

	@Override
	public Component getEnableMessage() {
		return Component.text("Streamer mode ", NamedTextColor.YELLOW)
			.append(Component.text("enabled", NamedTextColor.RED));
	}

	@Override
	public Component getDisableMessage() {
		return Component.text("Streamer Mode ", NamedTextColor.YELLOW)
			.append(Component.text("disabled", NamedTextColor.GREEN));
	}

	@Override
	public boolean assumeDefaultDisabled() {
		return true;
	}

	public static void register() {
		new StreamerModeCommand().registerCommand();
	}

	@Override
	public void setPerm(Player player, boolean setEnabled) { // Enabled = streamer mode
		if (setEnabled) {
			MonumentaNetworkChatIntegration.setPauseChat(player, true);
		}

		super.setPerm(player, setEnabled);

		if (!setEnabled) {
			MonumentaNetworkChatIntegration.setPauseChat(player, false);
		}
	}
}
