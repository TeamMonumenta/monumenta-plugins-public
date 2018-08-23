package com.playmonumenta.plugins.command.commands;

import java.util.Stack;

import com.playmonumenta.plugins.command.CommandContext;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import org.bukkit.plugin.Plugin;

public class Forward extends TeleportBase {

    public Forward(Plugin plugin) {
        super(
            "forward",
            "Transfers forward to location prior to /back, or multiple teleports prior. Saves coordinates to /back.",
            plugin
        );
    }

    @Override
    protected void configure(final ArgumentParser parser) {
        parser.addArgument("steps")
            .help("number of teleports forward")
            .type(Integer.class)
            .nargs("?")
            .setDefault(1);
    }

    @Override
    protected boolean run(final CommandContext context) {
        //noinspection OptionalGetWithoutIsPresent - checked before being called
        final Player player = context.getPlayer().get();
        final int numSteps = context.getNamespace().get("steps");
        final Stack<Location> backStack = getBackStack(player);
        final Stack<Location> forwardStack = getForwardStack(player);

		if (forwardStack.empty()) {
            sendErrorMessage(context,"No forward location to teleport to");
			return false;
		}

        final Location target = getTarget(player, numSteps, backStack, forwardStack);

        skipBackAdd(player);
        saveUpdatedStacks(player, forwardStack, backStack);

		player.teleport(target);
        sendMessage(context,"Teleporting forward" + (backStack.isEmpty() ? " (end of list)" : ""));

        return true;
	}
}
