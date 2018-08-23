package com.playmonumenta.plugins.command;

import net.sourceforge.argparse4j.inf.Namespace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CommandContext {
    private final CommandSender sender;
    private final Command command;
    private final Namespace namespace;
    private final Optional<Player> player;

    /**
     * Create a new command context.
     *
     * @param sender    the source of the command
     * @param command   the command which was executed
     * @param namespace the parsed arguments from the command
     */
    public CommandContext(CommandSender sender, Command command, Namespace namespace) {
        this.sender = sender;
        this.command = command;
        this.namespace = namespace;
        this.player = initPlayer(sender);
    }

    /**
     * Getter for the command sender.
     *
     * @return the sender
     */
    public CommandSender getSender() {
        return sender;
    }

    /**
     * Getter for the command object.
     *
     * @return the command
     */
    public Command getCommand() {
        return command;
    }

    /**
     * Getter for the argument namespace.
     *
     * @return the namespace
     */
    public Namespace getNamespace() {
        return namespace;
    }

    /**
     * Getter for player. Checks sender and proxy callee.
     *
     * @return a player if present
     */
    public Optional<Player> getPlayer() {
        return player;
    }

    /**
     * Whether or not the command was sent via a player.
     *
     * @return true if by player
     */
    public boolean isPlayerSender() {
        return sender instanceof Player;
    }

    /**
     * Whether or not the command was sent via console.
     *
     * @return true if by console
     */
    public boolean isConsoleSender() {
        return sender instanceof ConsoleCommandSender;
    }

    /**
     * Whether or not the command was sent via proxy. I.E. using /execute
     *
     * @return true if proxied
     */
    public boolean isProxiedSender() {
        return sender instanceof ProxiedCommandSender;
    }

    /**
     * Determine the Player if possible.
     *
     * @param sender the source of the command
     * @return null if not player
     */
    private static Optional<Player> initPlayer(final CommandSender sender) {
        Optional<Player> player = Optional.empty();

        if (sender instanceof Player) {
            player = Optional.of((Player) sender);
        } else if (sender instanceof ProxiedCommandSender) {
            CommandSender callee = ((ProxiedCommandSender) sender).getCallee();
            if (callee instanceof Player) {
                player = Optional.of((Player) callee);
            }
        }

        return player;
    }
}
