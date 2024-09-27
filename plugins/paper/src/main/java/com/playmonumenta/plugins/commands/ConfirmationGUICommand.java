package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.guis.ConfirmationGUI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ConfirmationGUICommand {
	private static final List<String> SELECTORS = Arrays.asList("@a", "@e", "@p", "@r", "@s");
	private static final List<String> OPTIONS = Arrays.asList("confirm:", "deny:");

	public static void register() {
		new CommandAPICommand("confirmationgui")
			.withArguments(new EntitySelectorArgument.ManyPlayers("targets").replaceSuggestions(ArgumentSuggestions.strings((info) -> {
				List<String> suggestions = new ArrayList<>(SELECTORS);
				for (Player player : Bukkit.getOnlinePlayers()) {
					suggestions.add(player.getName());
				}
				CommandSender sender = info.sender();
				if (sender instanceof LivingEntity senderEntity) {
					@Nullable Entity target = senderEntity.getTargetEntity(5);
					if (target != null) {
						suggestions.add(target.getUniqueId().toString());
					}
				}
				return suggestions.toArray(new String[0]);
			})))
			.withArguments(new GreedyStringArgument("argument").replaceSuggestions(ArgumentSuggestions.strings((info) -> {
				List<String> suggestions = new ArrayList<>(OPTIONS);
				String previousArgument = info.previousArgs().fullInput();
				String generatedSuggestion = generateSuggestion(previousArgument);
				if (generatedSuggestion != null) {
					suggestions.add(generatedSuggestion);
				}
				return suggestions.toArray(new String[0]);
			})))
			.withPermission("monumenta.command.confirmationgui")
			.executes((sender, args) -> {
				for (Object target : (Collection<?>) args.get("targets")) {
					if (target instanceof Player pTarget) {
						execute(pTarget, (String) args.get("argument"));
					}
				}
			})
			.register();
	}

	private static void execute(Player sender, String argument) {
		String confirmCommand = extractCommand("confirm:", argument);
		String denyCommand = extractCommand("deny:", argument);
		new ConfirmationGUI(sender, confirmCommand, denyCommand).open();
	}

	private static String extractCommand(String option, String argument) {
		Pattern pattern = Pattern.compile(option + "\\s*%([^%]+)%", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(argument);
		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		return "";
	}

	private static @Nullable String generateSuggestion(String argument) {
		String joinedOptions = String.join("|", OPTIONS);
		Pattern pattern = Pattern.compile("((" + joinedOptions + ")" + "(\\s|)(%|)([^%]+)(%|))", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(argument);
		if (matcher.find()) {
			String usedCommand = matcher.group(2);
			String newSuggestion = matcher.group(2);
			if (Objects.equals(matcher.group(6), "%")) {
				newSuggestion = matcher.group(1) + " ";
				newSuggestion += usedCommand.equals(OPTIONS.get(0)) ?
					OPTIONS.get(1) :
					OPTIONS.get(0);
			}
			newSuggestion += " %<whatever command>%";
			return newSuggestion;
		}
		return null;
	}
}
