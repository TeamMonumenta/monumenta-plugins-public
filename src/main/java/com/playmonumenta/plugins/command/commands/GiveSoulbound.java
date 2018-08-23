package com.playmonumenta.plugins.command.commands;

import com.playmonumenta.plugins.command.AbstractPlayerCommand;
import com.playmonumenta.plugins.command.CommandContext;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class GiveSoulbound extends AbstractPlayerCommand {

	public GiveSoulbound(Plugin plugin) {
		super(
		    "giveSoulbound",
		    "Appends the player's name into lore text on the given item",
		    plugin
		);
	}

	/**
	 * /execute @p[stuff] ~ ~ ~ givesoulbound item amount data datatag
	 *
	 * @param parser the {@link ArgumentParser} specific to the command
	 */
	@Override
	protected void configure(ArgumentParser parser) {
		parser.addArgument("item")
		.help("minecraft item id/name");
		parser.addArgument("amount")
		.help("number of item");
		parser.addArgument("data")
		.help("the item data");
		parser.addArgument("datatag")
		.help("the data tag");
	}

	@Override
	protected boolean run(CommandContext context) {
		//noinspection OptionalGetWithoutIsPresent - checked before being called
		final Player player = context.getPlayer().get();
		String consoleCommandString = context.getNamespace().getString("item") + " " +
		                              context.getNamespace().getString("amount") + " " +
		                              context.getNamespace().getString("data") + " " +
		                              context.getNamespace().getString("datatag");

		if (!consoleCommandString.contains("Lore:[")) {
			sendErrorMessage(context, "This command can only be used to give items that already have at least one line of lore text");
			return false;
		}

		// Append a line of lore text with the player's name
		consoleCommandString = consoleCommandString.replaceAll("(Lore:\\[[^]]*)]", "$1,\"* Soulbound to " + player.getName() + " *\"]");

		// Send the command to the console
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "give " + player.getName() + " " + consoleCommandString);

		/*
		give @a minecraft:stone_sword 1 0 {display:{Name:"§d§lWatcher's Sword",Lore:["* Unique Item *","§fQuis Custodeit Ipsos Custodets?"]},ench:[{id:17,lvl:1},{id:34,lvl:3}]}
		*/

		return true;
	}
}
