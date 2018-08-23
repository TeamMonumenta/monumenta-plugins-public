package com.playmonumenta.plugins.command.commands;

import com.playmonumenta.plugins.command.AbstractCommand;
import com.playmonumenta.plugins.command.CommandContext;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

public class TransferScores extends AbstractCommand {

    public TransferScores(Plugin plugin) {
        super(
            "transferScores",
            "Transfers Scoreboard values between one offline player to an online player. This is used to fix players changing names.",
            plugin
        );
    }

    @Override
    protected void configure(ArgumentParser parser) {
        parser.addArgument("from")
            .help("player name to copy from");
        parser.addArgument("to")
            .help("player name to copy to");
    }

    @Override
    protected boolean run(CommandContext context) {
        if (!context.isPlayerSender() && !context.isConsoleSender()) {
            sendErrorMessage(context, "This command can only be run by a player or the console");
            return false;
        }

        final String from = context.getNamespace().getString("from");
        final String to = context.getNamespace().getString("to");

        try {
            ScoreboardUtils.transferPlayerScores(from, to);
        } catch (Exception e) {
            sendErrorMessage(context, e.getMessage());
            return false;
        }

        sendMessage(context, ChatColor.GREEN + "Successfully transfered scoreboard values from '" + from + "' to '" + to + "'");

        return true;
    }
}
