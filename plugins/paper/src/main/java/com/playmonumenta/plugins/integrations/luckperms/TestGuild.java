package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.integrations.luckperms.listeners.GuildArguments;
import com.playmonumenta.plugins.utils.CommandUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;

public class TestGuild {
	public static void register() {

		// testguild <playername> <guild name>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.testguild");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new TextArgument("guild name")
			.replaceSuggestions(GuildArguments.NAME_SUGGESTIONS));

		new CommandAPICommand("testguild")
			.withArguments(arguments)
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, perms);
				run(args.getUnchecked("guild name"), args.getUnchecked("player"));
			})
			.register();
	}

	private static void run(String guildName, Player player) throws WrapperCommandSyntaxException {
		String guildId = GuildArguments.getIdFromName(guildName);
		if (guildId == null) {
			throw CommandAPI.failWithString("Could not identify guild by name: " + guildName);
		}

		Group guildRoot = LuckPermsIntegration.getGroup(guildId);
		if (guildRoot == null) {
			throw CommandAPI.failWithString("The guild " + guildName + " does not have a loaded group");
		}

		User user = LuckPermsIntegration.getUser(player);
		if (!GuildPermission.VISIT.hasAccess(guildRoot, user)) {
			throw CommandAPI.failWithString("Player '" + player.getName() + "' does not permission to visit this guild plot!");
		}

		if (LuckPermsIntegration.isLocked(guildRoot)) {
			throw CommandAPI.failWithString("The guild " + guildName + " is current on lockdown and cannot be entered!");
		}
	}
}
