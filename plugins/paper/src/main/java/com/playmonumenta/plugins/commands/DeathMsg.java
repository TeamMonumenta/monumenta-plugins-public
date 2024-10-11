package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

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
				return VISIBLE;
			} else if (score == 1) {
				return HIDDEN;
			} else {
				return VISIBLE;
			}
		}

		public static @Nullable DeathMessageState getDeathMessageState(@Nullable String cmdLiteral) {
			if (cmdLiteral == null) {
				return null;
			} else if (cmdLiteral.equals(HIDDEN.getCmdLiteral())) {
				return HIDDEN;
			} else {
				return VISIBLE;
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

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withOptionalArguments(new MultiLiteralArgument("state", DeathMessageState.VISIBLE.getCmdLiteral(), DeathMessageState.HIDDEN.getCmdLiteral()))
			.executes((sender, args) -> {
				run(sender, DeathMessageState.getDeathMessageState((String) args.get("state")));
			})
			.register();
	}

	private static void run(CommandSender sender, @Nullable DeathMessageState newState) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);

		if (newState != null) {
			// If a value was given, then update
			ScoreboardUtils.setScoreboardValue(player, Constants.SCOREBOARD_DEATH_MESSAGE, newState.getScore());
		} else {
			// Otherwise, get the existing value
			newState = DeathMessageState.getDeathMessageState(ScoreboardUtils.getScoreboardValue(player, Constants.SCOREBOARD_DEATH_MESSAGE).orElse(0));
		}

		player.sendMessage(Component.text("Death Message Settings", NamedTextColor.GOLD, TextDecoration.BOLD));
		player.sendMessage(Component.text("When you die, your death message will be shown to:", NamedTextColor.AQUA));
		player.sendMessage(Component.text("  " + newState.getDescription(), NamedTextColor.GREEN));
		player.sendMessage(Component.text("Change this with ", NamedTextColor.AQUA)
						   .append(Component.text("/deathmsg visible", NamedTextColor.GOLD))
						   .append(Component.text(" or ", NamedTextColor.AQUA))
						   .append(Component.text("/deathmsg hidden", NamedTextColor.GOLD)));
	}
}
