package com.playmonumenta.plugins.command.commands;

import com.playmonumenta.plugins.command.AbstractCommand;
import com.playmonumenta.plugins.command.CommandContext;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class RemoveTags extends AbstractCommand {

	public RemoveTags(Plugin plugin) {
		super(
		    "removeTags",
		    "Removes all of the target entity's scoreboard tags",
		    plugin
		);
	}

	@Override
	protected void configure(ArgumentParser parser) {
	}

	@Override
	protected boolean run(CommandContext context) {
		if (!context.isProxiedSender()) {
			sendErrorMessage(context, "This command can only be run on an entity/player via /execute!");
			return false;
		}

		CommandSender callee = ((ProxiedCommandSender) context.getSender()).getCallee();
		if (!(callee instanceof Entity)) {
			sendErrorMessage(context, "The target of this command must be an entity!");
			return false;
		}

		Entity target = (Entity) callee;

		target.getScoreboardTags().clear();

		if (callee instanceof Player) {
			sendMessage(context, "Cleared all tags from player '" + callee.getName() + "'");
		} else {
			sendMessage(context, "Cleared all tags from entity '" + target.getUniqueId() + "'");
		}

		return true;
	}
}
