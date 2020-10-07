package com.playmonumenta.plugins.integrations.luckperms;

import java.util.LinkedHashMap;

import com.playmonumenta.plugins.Plugin;

import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.LuckPermsApi;

public class TestGuild {
	public static void register(Plugin plugin, LuckPermsApi lp) {

		// testguild <playername>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.testguild");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));
		arguments.put("guild name", new TextArgument());

		new CommandAPICommand("testguild")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				run(lp, (String) args[1], (Player) args[0]);
			})
			.register();
	}

	private static void run(LuckPermsApi lp, String guildName, Player player) throws WrapperCommandSyntaxException {
		Group currentGuild = LuckPermsIntegration.getGuild(lp, player);
		String currentGuildName = LuckPermsIntegration.getGuildName(currentGuild);

		if (currentGuildName == null) {
			CommandAPI.fail("Player '" + player.getName() + "' is not in a guild!");
		} else if (!currentGuildName.equalsIgnoreCase(guildName)) {
			CommandAPI.fail("Player '" + player.getName() + "' is in other guild '" + currentGuildName + "'");
		}
	}
}
