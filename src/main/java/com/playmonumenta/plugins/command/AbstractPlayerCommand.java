package com.playmonumenta.plugins.command;

import org.bukkit.plugin.Plugin;

/**
 * Base class for command required to be run by/on a player.
 */
public abstract class AbstractPlayerCommand extends AbstractCommand {

    /**
     * Create the command.
     *
     * @param name        the command name
     * @param description the command description
     * @param plugin      the plugin object
     */
    public AbstractPlayerCommand(String name, String description, Plugin plugin) {
        super(name, description, plugin);
    }

    @Override
    protected boolean validate(CommandContext context) {
        if (!context.getPlayer().isPresent()) {
            sendErrorMessage(context, "This command must be run by/on a player!");
            return false;
        }

        return true;
    }
}
