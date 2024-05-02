package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.networkchat.channel.Channel;
import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class OffDutyCommand extends TogglePermCommand {
	public static String ON_DUTY_PERM_STRING = "monumenta.mod";

	@Override
	public String getCommandName() {
		return "offduty";
	}

	@Override
	public String getSetToDescription() {
		return "Set to Off Duty?";
	}

	@Override
	public String getCanTogglePerm() {
		return "group.mod";
	}

	@Override
	public String getToggledPerm() {
		return ON_DUTY_PERM_STRING;
	}

	@Override
	public Component getEnableMessage() {
		return Component.text("You are now ", NamedTextColor.YELLOW)
			.append(Component.text("on duty", NamedTextColor.RED));
	}

	@Override
	public Component getDisableMessage() {
		return Component.text("You are now ", NamedTextColor.YELLOW)
			.append(Component.text("off duty", NamedTextColor.GREEN));
	}

	@Override
	public boolean argIsInverted() {
		return true;
	}

	@Override
	public boolean assumeDefaultDisabled() {
		return true;
	}

	public static void register() {
		new OffDutyCommand().registerCommand();
	}

	@Override
	public void setPerm(Player player, boolean setEnabled) { // Enabled = off duty
		super.setPerm(player, setEnabled);

		// Update notification sounds for mh
		Channel modHelp = MonumentaNetworkChatIntegration.getChannel("mh");
		if (modHelp == null) {
			player.sendMessage(Component.text("Could not toggle notifications for mh channel", NamedTextColor.RED));
		} else {
			MonumentaNetworkChatIntegration.setPlayerChannelNotifications(player, modHelp, !setEnabled);
		}
	}
}
