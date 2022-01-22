package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class DeathMsg {
	private static final String COMMAND = "deathmsg";

	// NOTE: Default score is 0, which should be visible by default.
	public enum DeathMessageState {
		VISIBLE(0, "visible", "All players on the current shard"),
		HIDDEN(1, "hidden", "Only you");

		private final int mScore;
		private final String mCmdLiteral;
		private final String mDescription;

		DeathMessageState(int score, String cmdLiteral, String description) {
			this.mScore = score;
			this.mCmdLiteral = cmdLiteral;
			this.mDescription = description;
		}

		public static DeathMessageState getDeathMessageState(Integer score) {
			if (score == null) {
				return DeathMessageState.VISIBLE;
			} else if (score == 1) {
				return DeathMessageState.HIDDEN;
			} else {
				return DeathMessageState.VISIBLE;
			}
		}

		public static DeathMessageState getDeathMessageState(String cmdLiteral) {
			if (cmdLiteral == null) {
				return DeathMessageState.VISIBLE;
			} else if (cmdLiteral == DeathMessageState.HIDDEN.getCmdLiteral()) {
				return DeathMessageState.HIDDEN;
			} else {
				return DeathMessageState.VISIBLE;
			}
		}

		public int getScore() {
			return this.mScore;
		}

		public String getCmdLiteral() {
			return this.mCmdLiteral;
		}

		public String getDescription() {
			return this.mDescription;
		}
	}

	public static void register() {
		final CommandPermission perms = CommandPermission.fromString("monumenta.command.deathmsg");

		List<Argument> arguments = new ArrayList<>();
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.executes((sender, args) -> {
				run(sender, null);
			})
			.register();

		arguments.add(new MultiLiteralArgument(DeathMessageState.VISIBLE.getCmdLiteral(),
		                                       DeathMessageState.HIDDEN.getCmdLiteral()));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				run(sender, DeathMessageState.getDeathMessageState((String)args[0]));
			})
			.register();
	}

	private static void run(CommandSender sender, DeathMessageState newState) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);

		if (newState != null) {
			// If a value was given, then update
			ScoreboardUtils.setScoreboardValue(player, Constants.SCOREBOARD_DEATH_MESSAGE, newState.getScore());
		} else {
			// Otherwise, get the existing value
			newState = DeathMessageState.getDeathMessageState(ScoreboardUtils.getScoreboardValue(player, Constants.SCOREBOARD_DEATH_MESSAGE).orElse(0));
		}

		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Death Message Settings");
		player.sendMessage(ChatColor.AQUA + "When you die, your death message will be shown to:");
		player.sendMessage(ChatColor.GREEN + "  " + newState.getDescription());
		player.sendMessage(ChatColor.AQUA + "Change this with " + ChatColor.GOLD + "/deathmsg visible" +
		                   ChatColor.AQUA + " or " + ChatColor.GOLD + "/deathmsg hidden");
	}
}
