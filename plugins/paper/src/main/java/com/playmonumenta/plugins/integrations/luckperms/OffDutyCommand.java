package com.playmonumenta.plugins.integrations.luckperms;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.BooleanArgument;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.entity.Player;

public class OffDutyCommand {
	public static String ON_DUTY_PERM_STRING = "monumenta.mod";
	public static String MODERATOR_GROUP_STR = "mod";

	public static void register() {
		CommandPermission perms = CommandPermission.fromString("group." + MODERATOR_GROUP_STR);

		new CommandAPICommand("offduty")
			.withPermission(perms)
			.executesPlayer((player, args) -> {
				toggleOffDuty(player);
			})
			.register();

		new CommandAPICommand("offduty")
			.withPermission(perms)
			.withArguments(new BooleanArgument("set to off duty"))
			.executesPlayer((player, args) -> {
				setOffDuty(player, (boolean) args[0]);
			})
			.register();
	}

	public static void toggleOffDuty(Player player) {
		setOffDuty(player, player.hasPermission(ON_DUTY_PERM_STRING));
	}

	public static void setOffDuty(Player player, boolean markOffDuty) {
		PermissionNode onDutyNode = PermissionNode.builder().permission(ON_DUTY_PERM_STRING).build();
		PermissionNode offDutyNode = onDutyNode.toBuilder().value(false).build();

		User user = LuckPermsIntegration.getUser(player);
		NodeMap userData = user.data();

		userData.remove(onDutyNode);
		userData.remove(offDutyNode);

		if (markOffDuty) {
			userData.add(offDutyNode);
		}
		LuckPermsIntegration.pushUserUpdate(user);
	}
}
