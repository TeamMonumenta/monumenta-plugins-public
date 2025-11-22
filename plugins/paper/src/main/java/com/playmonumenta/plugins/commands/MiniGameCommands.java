package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.bosses.parameters.Parser;
import com.playmonumenta.plugins.bosses.parameters.Tokenizer;
import com.playmonumenta.plugins.bosses.parameters.Tokens;
import com.playmonumenta.plugins.listeners.MinigameManager;
import com.playmonumenta.plugins.minigames.Minigame;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.SafeSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MiniGameCommands {

	public static final String COMMAND = "minigame";

	public static Minigame.Arguments parseArguments(String string) throws Parser.ParseError {
		Tokens tokens = new Tokenizer(string).getTokens();
		Map<String, Double> map = new HashMap<>();
		while (true) {
			Tokens.Token name = tokens.consume(Tokens.TokenType.PARAMETER_NAME);
			tokens.consume(Tokens.TokenType.EQUALS);
			Parser.Result<Double> doubleResult = Parser.parseDouble(tokens);
			map.put(name.getValue(), doubleResult.data());
			if (tokens.hasRemaining()) {
				tokens.consume(Tokens.TokenType.COMMA);
				continue;
			}
			break;
		}
		return Minigame.Arguments.of(map);
	}

	public static void register() {
		new CommandAPICommand(COMMAND)
			.withPermission("monumenta.minigame")
			.withSubcommand(
				new CommandAPICommand("start")
					.withArguments(new StringArgument("id")
						.includeSuggestions(ArgumentSuggestions.strings(MinigameManager.getInstance().getMinigamesRegistered().keySet())))
					.withArguments(new LocationArgument("location"))
					.withArguments(new PlayerArgument("player"))
					.withArguments(new GreedyStringArgument("arguments")
						.replaceSafeSuggestions(SafeSuggestions.tooltipCollection(commandSenderSuggestionInfo -> {
							String s = commandSenderSuggestionInfo.currentArg();
							try {
								parseArguments(s);
							} catch (Parser.ParseError e) {
								return e.getSuggestions("");
							}
							return List.of();
						})))
					.executes((sender, args) -> {
						try {
							String id = args.getUnchecked("id");
							MinigameManager.getInstance().start(
								id,
								id,
								args.getUnchecked("location"),
								args.getUnchecked("player"),
								parseArguments(args.getUnchecked("arguments")));
						} catch (Parser.ParseError e) {
							throw CommandAPI.failWithString("Error: " + e.getMessage());
						}
					})
			).withSubcommand(
				new CommandAPICommand("start_custom")
					.withArguments(new StringArgument("id")
						.includeSuggestions(ArgumentSuggestions.strings(MinigameManager.getInstance().getMinigamesRegistered().keySet())))
					.withArguments(new StringArgument("custom_name"))
					.withArguments(new LocationArgument("location"))
					.withArguments(new PlayerArgument("player"))
					.withArguments(new GreedyStringArgument("arguments")
						.replaceSafeSuggestions(SafeSuggestions.tooltipCollection(commandSenderSuggestionInfo -> {
							String s = commandSenderSuggestionInfo.currentArg();
							try {
								parseArguments(s);
							} catch (Parser.ParseError e) {
								return e.getSuggestions("");
							}
							return List.of();
						})))
					.executes((sender, args) -> {
						try {
							MinigameManager.getInstance().start(
								args.getUnchecked("id"),
								args.getUnchecked("custom_name"),
								args.getUnchecked("location"),
								args.getUnchecked("player"),
								parseArguments(args.getUnchecked("arguments")));
						} catch (Parser.ParseError e) {
							throw CommandAPI.failWithString("Error: " + e.getMessage());
						}
					})
			)
			.withSubcommand(
				new CommandAPICommand("stop")
					.withArguments(new StringArgument("id")
						.includeSuggestions(ArgumentSuggestions.strings(MinigameManager.getInstance().getActiveMinigames().keySet())))
					.executes((sender, args) -> {
						MinigameManager.getInstance().stopMinigame(args.getUnchecked("id"));
					})
			)
			.withSubcommand(
				new CommandAPICommand("stopAll")
					.executes((sender, args) -> {
						MinigameManager.getInstance().stopActiveMinigames();
					})
			)
			.register();
	}
}
