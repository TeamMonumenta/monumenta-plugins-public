package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.luckperms.api.model.group.Group;
import org.bukkit.entity.Player;

public class TestGuild {
	public static void register(Plugin plugin) {

		// testguild <playername>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.testguild");

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
		arguments.add(new TextArgument("guild name"));

		new CommandAPICommand("testguild")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				run((String) args[1], (Player) args[0]);
			})
			.register();
	}

	private static void run(String guildName, Player player) throws WrapperCommandSyntaxException {
		Group currentGuild = LuckPermsIntegration.getGuild(player);
		String currentGuildName = LuckPermsIntegration.getGuildName(currentGuild);

		if (currentGuildName == null) {
			CommandAPI.fail("Player '" + player.getName() + "' is not in a guild!");
		} else if (!currentGuildName.equalsIgnoreCase(guildName)) {
			CommandAPI.fail("Player '" + player.getName() + "' is in other guild '" + currentGuildName + "'");
		}
	}
}
