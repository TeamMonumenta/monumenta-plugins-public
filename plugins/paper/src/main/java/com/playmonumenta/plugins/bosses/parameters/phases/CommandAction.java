package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import dev.jorel.commandapi.Tooltip;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.StringUtil;

public class CommandAction implements Action {

	private final String mCommand;

	public CommandAction(String command) {
		mCommand = command;
	}

	@Override public void runAction(LivingEntity boss) {
		Bukkit.dispatchCommand(
			Bukkit.getConsoleSender(),
			"execute as " + boss.getUniqueId() + " at " + boss.getUniqueId() + " run " + mCommand
		);
	}

	public static ParseResult<Action> fromReader(StringReader reader) {
		if (!reader.advance("(")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "(", "(...)")));
		}

		String soFar = reader.readSoFar();
		String commandFull = reader.readUntil(")");
		if (commandFull == null) {
			commandFull = reader.remaining();
		}

		String[] cmd = commandFull.split(" ", Integer.MAX_VALUE);
		List<Tooltip<String>> tooltips = new ArrayList<>();
		if (cmd.length == 1) {
			String prefix = cmd[0];
			ArrayList<String> completions = new ArrayList<>();
			for (String name : Bukkit.getCommandMap().getKnownCommands().keySet()) {
				if (StringUtil.startsWithIgnoreCase(name, prefix)) {
					completions.add(name);
				}
			}
			if (Bukkit.getCommandMap().getCommand(cmd[0]) != null) {
				tooltips.add(Tooltip.ofString(soFar + commandFull + ")", "end command"));
			}
			completions.stream()
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.map(name -> Tooltip.ofString(soFar + name, "command"))
				.forEach(tooltips::add);
		} else {
			Command commandC = Bukkit.getCommandMap().getCommand(cmd[0]);
			if (commandC == null) {
				return ParseResult.of(Tooltip.arrayOf());
			}
			tooltips.add(Tooltip.ofString(soFar + commandFull + ")", "end command"));

			List<String> tabComplete = commandC.tabComplete(Bukkit.getConsoleSender(), cmd[0], Arrays.copyOfRange(cmd, 1, cmd.length), null);
			String lastArg = cmd[cmd.length - 1];
			String suggestionsStart = tabComplete.stream().allMatch(tc -> StringUtils.startsWithIgnoreCase(tc, lastArg))
				                          ? soFar + commandFull.substring(0, commandFull.lastIndexOf(' ')) + ' ' : soFar + commandFull;
			tabComplete.stream()
				.map(s -> Tooltip.ofString(suggestionsStart + s, "command"))
				.forEach(tooltips::add);
		}

		if (!reader.advance(")")) {
			return ParseResult.of(tooltips.toArray(Tooltip.arrayOf()));
		}
		return ParseResult.of(new CommandAction(commandFull));

	}

}
