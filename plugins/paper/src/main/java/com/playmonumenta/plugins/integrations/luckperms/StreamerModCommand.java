package com.playmonumenta.plugins.integrations.luckperms;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class StreamerModCommand extends TogglePermCommand {
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
		new StreamerModCommand().registerCommand();
	}
}
