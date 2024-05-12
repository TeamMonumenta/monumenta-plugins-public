package com.playmonumenta.plugins.integrations.luckperms;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.BooleanArgument;
import net.kyori.adventure.text.Component;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.entity.Player;

public abstract class TogglePermCommand {
	public abstract String getCommandName();

	public abstract String getSetToDescription();

	public abstract String getCanTogglePerm();

	public abstract String getToggledPerm();

	public abstract Component getEnableMessage();

	public abstract Component getDisableMessage();

	public boolean argIsInverted() {
		return false;
	}

	public boolean assumeDefaultDisabled() {
		return false;
	}

	public boolean assumeDefaultEnabled() {
		return false;
	}

	public void registerCommand() {
		CommandPermission modPerms = CommandPermission.fromString(getCanTogglePerm());

		new CommandAPICommand(getCommandName())
			.withPermission(modPerms)
			.executesPlayer((player, args) -> {
				togglePerm(player);
			})
			.register();

		BooleanArgument booleanArg = new BooleanArgument(getSetToDescription());

		new CommandAPICommand(getCommandName().replace(" ", ""))
			.withPermission(modPerms)
			.withArguments(booleanArg)
			.executesPlayer((player, args) -> {
				setPerm(player, argIsInverted() ^ args.getByArgument(booleanArg));
			})
			.register();
	}

	public void togglePerm(Player player) {
		setPerm(player, !player.hasPermission(getToggledPerm()));
	}

	public void setPerm(Player player, boolean setEnabled) {
		// Update tab list
		Node permEnabledNode;
		Node permDisabledNode;

		String perm = getToggledPerm();
		if (perm.startsWith("group.")) {
			perm = perm.substring(6);
			permEnabledNode = InheritanceNode.builder(perm).build();
			permDisabledNode = null;
		} else {
			permEnabledNode = PermissionNode.builder().permission(getToggledPerm()).build();
			permDisabledNode = permEnabledNode.toBuilder().value(false).build();
		}

		User user = LuckPermsIntegration.getUser(player);
		NodeMap userData = user.data();

		userData.remove(permEnabledNode);
		if (permDisabledNode != null) {
			userData.remove(permDisabledNode);
		}

		if (setEnabled) {
			if (!assumeDefaultEnabled()) {
				userData.add(permEnabledNode);
			}
			player.sendMessage(getEnableMessage());
		}

		if (!setEnabled) {
			if (!assumeDefaultDisabled() && permDisabledNode != null) {
				userData.add(permDisabledNode);
			}
			player.sendMessage(getDisableMessage());
		}

		LuckPermsIntegration.pushUserUpdate(user);
	}
}
