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
import org.bukkit.entity.Player;

// TODO Legacy, update this
public class TestGuild {
	public static void register() {

		// testguild <playername>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.testguild");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new TextArgument("guild name")
			.replaceSuggestions(GuildArguments.NAME_SUGGESTIONS));

		new CommandAPICommand("testguild")
			.withArguments(arguments)
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, perms);
				run((String) args[1], (Player) args[0]);
			})
			.register();
	}

	private static void run(String guildName, Player player) throws WrapperCommandSyntaxException {
		String cleanName = GuildArguments.getIdFromName(guildName);
		if (cleanName == null) {
			throw CommandAPI.failWithString("Could not identify guild by name: " + guildName);
		}
		String guestPermName = GuildAccessLevel.GUEST.groupNameFromRoot(cleanName);

		if (!player.hasPermission("group." + guestPermName)) {
			throw CommandAPI.failWithString("Player '" + player.getName() + "' does not have guest access to this guild!");
		}

		Group guestGroup = LuckPermsIntegration.getGroup(guestPermName);
		if (guestGroup == null) {
			throw CommandAPI.failWithString("The guild " + guildName + " does not have a loaded guest group?");
		}
		if (LuckPermsIntegration.isLocked(guestGroup)) {
			throw CommandAPI.failWithString("The guild " + guildName + " is current on lockdown and cannot be entered!");
		}
	}
}
